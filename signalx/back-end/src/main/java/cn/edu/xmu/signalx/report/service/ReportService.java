package cn.edu.xmu.signalx.report.service;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.edu.xmu.signalx.core.notify.MailNotificationService;
import cn.edu.xmu.signalx.core.redis.queue.QueueMessage;
import cn.edu.xmu.signalx.core.redis.queue.RedisQueue;
import cn.edu.xmu.signalx.cos.service.CosService;
import cn.edu.xmu.signalx.report.domain.model.Report;
import cn.edu.xmu.signalx.report.domain.model.ReportStatus;
import cn.edu.xmu.signalx.report.domain.repository.ReportRepository;
import cn.edu.xmu.signalx.report.infrastructure.client.CozeClient;
import cn.edu.xmu.signalx.record.domain.model.News;
import cn.edu.xmu.signalx.record.service.NewsService;
import cn.edu.xmu.signalx.task.domain.model.Task;
import cn.edu.xmu.signalx.task.domain.repository.TaskRepository;
import cn.edu.xmu.signalx.user.domain.model.User;
import cn.edu.xmu.signalx.user.domain.repository.UserRepository;
import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报表服务核心类
 * 核心功能：
 * 1. 触发指定任务的报表生成（基于任务createTime筛选，只查任务创建后的新闻）
 * 2. 对接Coze API生成报表内容
 * 3. 报表内容COS上传、邮件推送
 * 4. 报表状态管理、手动推送/删除等
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    // 核心依赖注入
    private final ReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final cn.edu.xmu.signalx.task.domain.repository.OpenclawTaskRepository openclawTaskRepository;
    private final NewsService newsService;
    private final CozeClient reportClient;
    private final UserRepository userRepository;
    private final MailNotificationService mailNotificationService;
    private final CosService cosService;
    private final RedisQueue redisQueue;

    // JdbcTemplate - 读取数据库最新关键词
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * Executes report generation for all due tasks.
     * Invoked by scheduler.
     */
    public void executeDueReports() {
        // Here we assume "due" tasks are active tasks that have reached their next run time.
        // Or we could have a specific query in taskRepository for due tasks.
        List<Task> dueTasks = taskRepository.findDueTasks(LocalDateTime.now());
        if (dueTasks.isEmpty()) {
            return;
        }

        for (Task task : dueTasks) {
            try {
                // 将任务推送到 Redis 队列，异步处理
                QueueMessage<Long> message = QueueMessage.create("report.generate", task.getId());
                redisQueue.push(message);
                log.info("报表生成任务已入队 | taskId={}, msgId={}", task.getId(), message.getMessageId());
            } catch (Exception e) {
                log.error("Failed to queue report task for task [{}]", task.getId(), e);
            }
        }
    }

    /**
     * Synchronizes status of all reports that are currently in GENERATING state.
     * Invoked by scheduler.
     */
    public void syncReportsStatus() {
        List<Report> generatingReports = reportRepository.findByStatus(ReportStatus.GENERATING);
        for (Report report : generatingReports) {
            try {
                syncSingleReportStatus(report);
            } catch (Exception e) {
                log.error("Failed to sync status for report [{}]", report.getId(), e);
            }
        }
    }

    @Transactional
    public Long createReport(Long taskId, String name) {
        return createReportWithType(taskId, 0, name);
    }

    @Transactional
    public Long createReportWithType(Long taskId, Integer taskType, String name) {
        Report report = Report.create(taskId, taskType, name);
        return reportRepository.save(report);
    }

    @Transactional
    public void completeReport(Long reportId, String summary) {
        Report report = reportRepository.findById(reportId);
        if (report == null) {
            throw new DomainException("Report not found");
        }
        report.complete(summary);
        reportRepository.save(report);
    }

    private void syncSingleReportStatus(Report report) {
        CozeClient.CozeResult result = reportClient.query(report.getCozeRunId());
        if (result.isCompleted()) {
            report.complete(result.getMarkdown());
            reportRepository.save(report);
            log.info("Report [{}] generation completed via sync", report.getId());
            // Optional: send email
            trySendReportEmail(report);
        } else if (result.isFailed()) {
            report.fail();
            reportRepository.save(report);
            log.error("Report [{}] generation failed via sync", report.getId());
        }
    }

    /**
     * 触发指定任务的报表生成流程
     * @param taskId 任务ID
     * @param taskType 任务类型 (0: 标准, 1: Openclaw)
     */
    @Transactional(rollbackFor = Exception.class)
    public void triggerReportForTask(Long taskId, Integer taskType) {
        if (taskType == null) taskType = 0;

        List<String> latestKeywords = new ArrayList<>();
        LocalDateTime taskCreateTime = null;
        String taskName = "通用";

        // 分支校验任务是否存在并获取信息
        if (taskType == 0) {
            Task task = taskRepository.findById(taskId);
            if (task == null) {
                log.error("标准任务不存在，终止报表生成 | taskId={}", taskId);
                return;
            }
            // 时间校验
            if (task.getNextRunTime() != null && task.getNextRunTime().isAfter(LocalDateTime.now())) {
                log.warn("任务未到执行时间，跳过触发 | taskId={}, 下次执行时间={}", taskId, task.getNextRunTime());
                return;
            }
            task.advanceSchedule();
            taskRepository.save(task);

            taskCreateTime = task.getCreateTime();
            taskName = "日常监测";

            List<String> dbKeywords = getLatestKeywordsFromDb(taskId);
            latestKeywords = dbKeywords != null ? dbKeywords : task.getKeywords();
            if (latestKeywords == null) latestKeywords = new ArrayList<>();

        } else if (taskType == 1) {
            cn.edu.xmu.signalx.task.domain.model.OpenclawTask oTask = openclawTaskRepository.findById(taskId);
            if (oTask == null) {
                log.error("Openclaw任务不存在，终止报表生成 | taskId={}", taskId);
                return;
            }
            // 时间校验
            if (oTask.getNextRunTime() != null && oTask.getNextRunTime().isAfter(LocalDateTime.now())) {
                log.warn("Openclaw任务未到执行时间，跳过触发 | taskId={}, 下次执行时间={}", taskId, oTask.getNextRunTime());
                return;
            }
            oTask.advanceSchedule();
            openclawTaskRepository.save(oTask);

            taskCreateTime = oTask.getCreateTime();
            taskName = oTask.getName();
            latestKeywords.add(oTask.getName()); // OpenClaw 无 keywords，取任务名作为 fallback
        }

        // 生成唯一报表名 (年月日时 + 关键词报告)
        String timeDisplay = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH时"));
        String keyword = latestKeywords.isEmpty() ? taskName : latestKeywords.get(0);
        if (latestKeywords.size() > 1) {
            keyword += "等";
        }
        String reportName = String.format("%s - %s 舆情分析报告", timeDisplay, keyword);

        // 校验报表名是否重复
        if (reportRepository.findByTaskIdAndTypeAndName(taskId, taskType, reportName) != null) {
            log.warn("重复报表已存在，跳过创建 | taskId={}, taskType={}, 报表名={}", taskId, taskType, reportName);
            return;
        }

        // 初始化报表 (根据传入的 taskType 类型)
        Report report = Report.create(taskId, taskType, reportName);
        Long reportId = reportRepository.save(report);
        if (reportId == null) {
            reportId = report.getId();
        }
        if (reportId == null) {
            log.error("创建新报表失败（未返回ID），终止后续流程 | taskId={}, taskType={} | 报表名={}", taskId, taskType, reportName);
            return;
        }
        log.info("创建新报表成功 | reportId={} | taskId={}, taskType={} | 报表名={}", reportId, taskId, taskType, reportName);

        // ========== 核心修改：基于任务createTime和taskType筛选，只查任务创建后的对应新闻 ==========
       // 调用NewsService的listNews（注意以后要传taskType，目前需要我们在外部根据taskType去过滤或者扩展NewsService）
        PageResult<News> newsPage = newsService.listNews(taskId, taskType, null, null, 1, 50);
         List<News> newsList = newsPage.getList();

// 筛选逻辑：只保留 news.publishTime > 任务createTime 的新闻（用publishTime替代createTime）
        final LocalDateTime finalTaskCreateTime = taskCreateTime;
        List<News> filteredNewsList = newsList.stream()
        .filter(news -> news.getPublishTime() != null  // 改用publishTime
                && finalTaskCreateTime != null && news.getPublishTime().isAfter(finalTaskCreateTime)) // 只查任务创建后的新闻
        .collect(Collectors.toList());

        log.info("读取任务关联新闻 | taskId={} | 原始新闻数={} | 过滤后（任务创建后）新闻数={}",
         taskId, newsList.size(), filteredNewsList.size());
         newsList = filteredNewsList;

        // 无新闻时生成空报表
        if (newsList.isEmpty()) {
            log.info("任务无关联新闻，生成空报表 | reportId={} | taskId={}", reportId, taskId);
            report.complete("本监测周期内未发现相关新闻报道。");
            reportRepository.save(report);
            trySendReportEmail(report);
            return;
        }

        // 对接Coze生成报表
        try {
            // 转换新闻格式（修复字段名、时间格式、补充缺失字段）
            List<Map<String, Object>> mappedNews = convertNewsToCozeFormat(newsList);

            // 提交Coze任务（关键词已通过参数传入，需确保CozeClient内使用中文字段名）
            String cozeRunId = reportClient.submitTask(latestKeywords, mappedNews);
            log.info("Coze任务提交成功 | reportId={} | runId={}", reportId, cozeRunId);

            // 绑定 Coze 任务 ID 并通过 Redis 队列异步轮询结果
            if (cozeRunId != null) {
                report.bindCozeRunId(cozeRunId);
                reportRepository.save(report);
                // 推送到 Redis 队列，异步处理轮询
                QueueMessage<Long> message = QueueMessage.create("report.poll", reportId);
                redisQueue.push(message);
                log.info("Coze 任务轮询任务已入队 | reportId={}, msgId={}", reportId, message.getMessageId());
            } else {
                log.error("Coze 任务提交失败（返回 runId 为空） | reportId={}", reportId);
                report.fail();
                reportRepository.save(report);
            }
        } catch (Exception e) {
            log.error("Coze任务提交异常 | reportId={} | taskId={}", reportId, taskId, e);
            report.fail();
            reportRepository.save(report);
        }
    }

    /**
     * 从数据库读取最新关键词
     * @param taskId 任务ID
     * @return 最新关键词列表
     */
    private List<String> getLatestKeywordsFromDb(Long taskId) {
        if (jdbcTemplate == null) {
            return null;
        }
        String sql = "SELECT keywords FROM task WHERE id = ?";
        try {
            String keywordsJson = jdbcTemplate.queryForObject(sql, String.class, taskId);
            List<String> keywords = JSON.parseArray(keywordsJson, String.class);
            log.info("读取最新关键词 | taskId={} | 关键词={}", taskId, keywords);
            return keywords == null ? new ArrayList<>() : keywords;
        } catch (Exception e) {
            log.error("读取数据库关键词失败 | taskId={}", taskId, e);
            return null;
        }
    }

    /**
     * 转换新闻格式为Coze API要求的格式
     * 修复点：
     * 1. 字段名英文→中文
     * 2. 时间格式ISO 8601 → YYYY-MM-DD
     * 3. 补充缺失字段（作者、热度、情感）
     * 4. 关键词通过外层参数传入CozeClient
     */
    private List<Map<String, Object>> convertNewsToCozeFormat(List<News> newsList) {
        // 时间格式化器：ISO 8601 → YYYY-MM-DD
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return newsList.stream().map(news -> {
            Map<String, Object> newsMap = new HashMap<>();

            // ========== 修复1：字段名英文→中文 ==========
            newsMap.put("标题", news.getTitle()); // title → 标题

            // 内容截断（避免超长）
            String content = news.getContent();
            if (content != null && content.length() > 500) {
                content = content.substring(0, 500) + "...";
            }
            newsMap.put("内容", content); // content → 内容

            // ========== 修复2：时间格式转换 ==========
            String publishTime = "";
            if (news.getPublishTime() != null) {
                publishTime = news.getPublishTime().format(dateFormatter); // ISO 8601 → YYYY-MM-DD
            }
            newsMap.put("发布时间", publishTime); // publish_time → 发布时间

            newsMap.put("来源", news.getSource()); // source → 来源

            // ========== 修复3：补充缺失字段（作者、热度、情感） ==========
            // 作者字段（无数据则默认"未知"）
            newsMap.put("作者", "未知"); // 若News无author字段，先写死默认值
            // 热度字段（无数据则默认0）
            newsMap.put("热度", 0); // 若News无hotScore字段，先写死默认值
            // 情感字段（无数据则默认"中性"）
            newsMap.put("情感", "中性"); // 若News无sentiment字段，先写死默认值

            return newsMap;
        }).collect(Collectors.toList());
    }

    /**
     * 处理完成的报表（COS 上传 + 邮件推送）
     * @param reportId 报表 ID
     * @param summary 报表内容（Markdown）
     */
    public void processCompletedReport(Long reportId, String summary) {
        Report report = reportRepository.findById(reportId);
        if (report == null) {
            log.error("报表不存在，跳过处理 | reportId={}", reportId);
            return;
        }

        try {
            // 标记报表为完成
            report.complete(summary);

            // COS上传报表文件
            if (cosService.isAvailable()) {
                Task task = taskRepository.findById(report.getTaskId());
                String userId = task != null ? task.getUserId().toString() : "default";
                String fileUrl = cosService.storeReport(report.getName(), summary, userId);
                report.setFileUrl(fileUrl);
                log.info("报表COS上传成功 | reportId={} | 文件地址={}", reportId, fileUrl);
            }

            // 保存报表并发送邮件
            reportRepository.save(report);
            trySendReportEmail(report);
            log.info("报表处理完成 | reportId={} | 状态=COMPLETED", reportId);
        } catch (Exception e) {
            log.error("处理完成报表异常 | reportId={}", reportId, e);
            report.fail();
            reportRepository.save(report);
        }
    }

    /**
     * 尝试发送报表邮件（内部调用，失败不抛异常）
     * @param report 报表对象
     */
    private void trySendReportEmail(Report report) {
        try {
            Task task = taskRepository.findById(report.getTaskId());
            if (task == null) {
                log.warn("任务不存在，跳过邮件发送 | reportId={}", report.getId());
                return;
            }

            User user = userRepository.findById(task.getUserId());
            String to = user != null ? user.getEmail() : null;
            if (to == null || to.isEmpty()) {
                log.warn("用户邮箱为空，跳过邮件发送 | reportId={}", report.getId());
                return;
            }

            // 组装邮件内容（含COS链接）
            String content = report.getSummary();
            if (report.getFileUrl() != null) {
                content += "\n\n报表文件链接：" + report.getFileUrl();
            }

            // 发送邮件
            mailNotificationService.sendReportCompleted(to, report.getName(), report.getTaskId(), report.getId(), content);
            log.info("报表邮件发送成功 | reportId={} | 收件人={}", report.getId(), to);
        } catch (Exception e) {
            log.warn("报表邮件发送失败 | reportId={}", report.getId(), e);
        }
    }

    /**
     * 同步处理所有未完成的报表（兜底逻辑）
     */
    @Transactional(rollbackFor = Exception.class)
    public void syncPendingReports() {
        List<Report> pendingReports = reportRepository.findGeneratingWithCozeId();
        log.info("开始同步未完成报表 | 待处理数量={}", pendingReports.size());

        for (Report report : pendingReports) {
            try {
                CozeClient.CozeResult result = reportClient.query(report.getCozeRunId());
                if (result.isCompleted()) {
                    processCompletedReport(report.getId(), result.getMarkdown());
                    log.info("同步报表完成 | reportId={}", report.getId());
                } else if (result.isFailed()) {
                    report.fail();
                    reportRepository.save(report);
                    log.info("同步报表标记为失败 | reportId={}", report.getId());
                }
            } catch (Exception e) {
                log.error("同步报表异常 | reportId={}", report.getId(), e);
            }
        }
    }

    /**
     * 手动推送报表邮件
     * @param userId 用户ID（权限校验）
     * @param reportId 报表ID
     * @return 推送结果（true=成功，false=失败）
     */
    public boolean pushReportEmail(Long userId, Long reportId) {
        // 权限校验 + 基础校验
        Report report = reportRepository.findById(reportId);
        if (report == null) throw new DomainException("报表不存在");

        Task task = taskRepository.findById(report.getTaskId());
        if (task == null) throw new DomainException("任务不存在");
        task.validateOwnership(userId);

        if (!mailNotificationService.isEnabled()) {
            throw new DomainException("邮件通知功能未启用");
        }

        // 组装邮件内容
        String content = report.getSummary() == null || report.getSummary().isBlank()
                ? "(报表内容为空)\nstatus=" + report.getStatus() + "\ncozeRunId=" + report.getCozeRunId()
                : report.getSummary();

        try {
            User user = userRepository.findById(task.getUserId());
            String to = user != null ? user.getEmail() : null;
            mailNotificationService.sendReportCompleted(to, report.getName(), task.getId(), reportId, content);
            log.info("手动推送报表邮件成功 | reportId={} | 收件人={}", reportId, to);
            return true;
        } catch (Exception e) {
            log.warn("手动推送报表邮件失败 | reportId={}", reportId, e);
            return false;
        }
    }

    /**
     * 分页查询报表列表（权限校验）
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param page 页码（从1开始）
     * @param size 页大小
     * @return 报表分页结果
     */
    /**
     * 获取指定任务的报表列表
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param taskType 任务类型 (0: 标准, 1: Openclaw)
     * @return 报表分页结果
     */
    public PageResult<Report> listReports(Long userId, Long taskId, Integer taskType, int page, int size) {
        if (taskType == null || taskType == 0) {
            // 类型为 0：查标准 Task 表
            Task task = taskRepository.findById(taskId);
            if (task == null) throw new DomainException("标准监测任务不存在");
            task.validateOwnership(userId);
        } else {
            // 类型为 1：查 OpenClaw 系统任务
            cn.edu.xmu.signalx.task.domain.model.OpenclawTask oTask = openclawTaskRepository.findById(taskId);
            if (oTask == null) throw new DomainException("Openclaw监测任务不存在");
            // 权限校验
            if (!oTask.getUserId().equals(userId)) {
                throw new DomainException("无权查看该任务的报表");
            }
        }

        // 精确根据 taskId 和 taskType 查询 report 表
        return reportRepository.findByTaskIdAndType(taskId, taskType, page, size);
    }

    /**
     * 获取报表详情（权限校验）
     * @param userId 用户ID
     * @param reportId 报表ID
     * @return 报表详情
     */
    public Report getReportDetail(Long userId, Long reportId) {
        Report report = reportRepository.findById(reportId);
        if (report == null) throw new DomainException("报表不存在");

        Task task = taskRepository.findById(report.getTaskId());
        task.validateOwnership(userId);
        return report;
    }

    /**
     * 删除报表（权限校验）
     * @param userId 用户ID
     * @param reportId 报表ID
     */
    public void deleteReport(Long userId, Long reportId) {
        Report report = reportRepository.findById(reportId);
        if (report == null) {
            log.warn("报表不存在，跳过删除 | reportId={}", reportId);
            return;
        }

        Task task = taskRepository.findById(report.getTaskId());
        task.validateOwnership(userId);
        reportRepository.delete(reportId);
        log.info("报表删除成功 | reportId={} | taskId={}", reportId, report.getTaskId());
    }
}

package cn.edu.xmu.signalx.report.domain.model;

import cn.edu.xmu.signalx.core.exception.DomainException;
import lombok.Getter;
import java.time.LocalDateTime;

/**
 * 报表领域模型
 * 核心属性：报表ID、任务ID、名称、内容、COS地址、状态等
 */
@Getter
public class Report {
    // 核心字段
    private Long id;
    private Long taskId;
    private Integer taskType;     // 0: MONITORING, 1: OPENCLAW
    private String name;
    private String summary;       // 报表内容（Markdown格式）
    private String cozeRunId;     // Coze任务ID
    private String fileUrl;       // COS文件地址
    private ReportStatus status;  // 报表状态
    private LocalDateTime createTime; // 创建时间

    // 私化构造函数，通过工厂方法创建
    public Report() {}

    /**
     * 设置报表ID（仅仓储层使用）
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 创建新报表（工厂方法）
     * @param taskId 任务ID
     * @param taskType 任务类型 (0:MONITORING, 1:OPENCLAW)
     * @param name 报表名称
     * @return 新报表实例
     */
    public static Report create(Long taskId, Integer taskType, String name) {
        Report report = new Report();
        report.taskId = taskId;
        report.taskType = taskType != null ? taskType : 0;
        report.name = name;
        report.status = ReportStatus.GENERATING;
        report.createTime = LocalDateTime.now();
        return report;
    }

    /**
     * 重构报表实例（从数据库加载）
     */
    public static Report reconstruct(Long id, Long taskId, Integer taskType, String name, String summary, String cozeRunId, String fileUrl, Integer status, LocalDateTime createTime) {
        Report report = new Report();
        report.id = id;
        report.taskId = taskId;
        report.taskType = taskType != null ? taskType : 0;
        report.name = name;
        report.summary = summary;
        report.cozeRunId = cozeRunId;
        report.fileUrl = fileUrl;
        report.status = ReportStatus.fromCode(status);
        report.createTime = createTime;
        return report;
    }

    /**
     * 设置COS文件地址
     */
    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    /**
     * 绑定Coze任务ID
     * @throws DomainException 报表已完成/失败时抛出异常
     */
    public void bindCozeRunId(String cozeRunId) {
         if (this.status != ReportStatus.GENERATING) {
            throw new DomainException("报表已完成/失败，无法绑定Coze任务ID");
        }
        this.cozeRunId = cozeRunId;
    }

    /**
     * 完成报表生成
     * @param finalMarkdown 最终报表内容（Markdown格式）
     * @throws DomainException 状态非法/内容为空时抛出异常
     */
    public void complete(String finalMarkdown) {
        // 状态校验：仅生成中的报表可完成
        if (this.status == ReportStatus.COMPLETED) {
            this.summary = finalMarkdown; // 兼容重复完成场景，更新最新内容
            return;
        }
        if (this.status != ReportStatus.GENERATING) {
            throw new DomainException("非法状态：仅生成中的报表可完成 | 当前状态=" + this.status);
        }

        // 内容校验：不能为空
        if (finalMarkdown == null || finalMarkdown.isBlank()) {
            throw new DomainException("报表内容不能为空");
        }

        // 更新状态和内容
        this.summary = finalMarkdown;
        this.status = ReportStatus.COMPLETED;
    }

    /**
     * 标记报表生成失败
     */
    public void fail() {
        if (this.status == ReportStatus.GENERATING) {
            this.status = ReportStatus.FAILED;
        }
    }

    /**
     * 报表状态使用同包下的 {@link cn.edu.xmu.signalx.report.domain.model.ReportStatus}
     */
}

package cn.edu.xmu.signalx.task.domain.model;

import cn.edu.xmu.signalx.core.exception.DomainException;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Openclaw 定时任务领域模型
 */
@Getter
public class OpenclawTask {
    private Long id;
    private Long userId;
    private String name;           // 任务名称
    private String url;            // 定制的网站URL
    private String crawlLogic;     // 爬取的业务逻辑 (自定义文本)
    private TaskConfig config;     // 复用现有配置 (startTime, endTime, interval)
    private TaskStatus status;     // 状态 (ACTIVE/INACTIVE)
    private LocalDateTime createTime;
    private LocalDateTime nextRunTime;

    private OpenclawTask() {}

    // Factory Method
    public static OpenclawTask create(Long userId, String name, String url, String logic,
                                     LocalDateTime start, LocalDateTime end, Integer interval) {
        validateUrl(url);
        if (name == null || name.isEmpty()) {
            throw new DomainException("Task name cannot be empty");
        }

        OpenclawTask task = new OpenclawTask();
        task.userId = userId;
        task.name = name;
        task.url = url;
        task.crawlLogic = logic;
        task.config = new TaskConfig(start, end, interval);
        task.status = TaskStatus.ACTIVE;
        task.createTime = LocalDateTime.now();
        // 初始策略：任务创建后，经过一个 interval 周期再执行
        task.nextRunTime = task.createTime.plusMinutes(interval != null ? interval : 1440);
        return task;
    }

    // Reconstruction
    public static OpenclawTask reconstruct(Long id, Long userId, String name, String url, String logic,
                                          TaskConfig config, TaskStatus status,
                                          LocalDateTime createTime, LocalDateTime nextRunTime) {
        OpenclawTask task = new OpenclawTask();
        task.id = id;
        task.userId = userId;
        task.name = name;
        task.url = url;
        task.crawlLogic = logic;
        task.config = config;
        task.status = status;
        task.createTime = createTime;
        task.nextRunTime = nextRunTime;
        return task;
    }

    private static void validateUrl(String url) {
        if (url == null || (!url.startsWith("http://") && !url.startsWith("https://"))) {
            throw new DomainException("Invalid URL format: " + url);
        }
    }

    // Behavior: 权限校验
    public void validateOwnership(Long operatorId) {
        if (!this.userId.equals(operatorId)) {
            throw new DomainException("Permission denied: You do not own this task.");
        }
    }

    // Behavior: 更新配置
    public void update(String name, String url, String logic, Integer interval) {
        validateUrl(url);
        this.name = name;
        this.url = url;
        this.crawlLogic = logic;
        this.config = this.config.update(null, null, interval);
    }

    // Behavior: 启停
    public void switchStatus(boolean enable) {
         if (enable) {
            this.status = TaskStatus.ACTIVE;
            this.nextRunTime = LocalDateTime.now().plusMinutes(this.config.getInterval());
        } else {
            this.status = TaskStatus.INACTIVE;
            this.nextRunTime = null;
        }
    }

    // Behavior: 推进调度
    public void advanceSchedule() {
        if (this.nextRunTime == null) {
            this.nextRunTime = LocalDateTime.now();
        }
        this.nextRunTime = LocalDateTime.now().plusMinutes(this.config.getInterval());
    }
}

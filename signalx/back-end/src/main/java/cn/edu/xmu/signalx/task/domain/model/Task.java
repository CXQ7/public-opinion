package cn.edu.xmu.signalx.task.domain.model;

import cn.edu.xmu.signalx.core.exception.DomainException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class Task {
    private Long id;
    private Long userId;
    private List<String> keywords;
    private TaskConfig config; // VO: 包含 startTime, endTime, interval
    private TaskStatus status;
    private LocalDateTime createTime;
    private LocalDateTime nextRunTime;

    // Factory Method
    // 参数直接接收具体时间对象
    public static Task create(Long userId, List<String> keywords, LocalDateTime startTime, LocalDateTime endTime, Integer interval) {
        validateKeywords(keywords);
        Task task = new Task();
        task.userId = userId;
        task.keywords = keywords;
        // 创建 VO
        task.config = new TaskConfig(startTime, endTime, interval);
        task.status = TaskStatus.ACTIVE;
        task.createTime = LocalDateTime.now();
        // 初始策略：任务创建后，经过一个 interval 周期再出第一份报告
        task.nextRunTime = task.createTime.plusMinutes(interval != null ? interval : 1440);
        return task;
    }

    // Reconstruction
    public static Task reconstruct(Long id, Long userId, List<String> keywords, TaskConfig config, TaskStatus status, LocalDateTime createTime, LocalDateTime nextRunTime) {
        Task task = new Task();
        task.id = id;
        task.userId = userId;
        task.keywords = keywords;
        task.config = config;
        task.status = status;
        task.createTime = createTime;
        task.nextRunTime = nextRunTime;
        return task;
    }

    // Behavior: 更新配置
    public void updateConfig(List<String> newKeywords, LocalDateTime newStartTime, LocalDateTime newEndTime, Integer newInterval) {
        if (newKeywords != null && !newKeywords.isEmpty()) {
            validateKeywords(newKeywords);
            this.keywords = newKeywords;
        }
        // 委托给 VO 处理不可变更新
        this.config = this.config.update(newStartTime, newEndTime, newInterval);

        // 如果 interval 发生变化，或者是重启任务，可能需要重置 nextRunTime
        // 这里简单处理：如果当前启用且 nextRunTime 在过去，则推进到将来
        if (this.status == TaskStatus.ACTIVE && newInterval != null) {
             // 简单的重置策略：基于当前时间 + 新间隔
             this.nextRunTime = LocalDateTime.now().plusMinutes(this.config.getInterval());
        }
    }

    // Behavior: 启停
    public void switchStatus(boolean enable) {
         if (enable) {
            this.status = TaskStatus.ACTIVE;
            // 重新启用时，重置下一次执行时间
            this.nextRunTime = LocalDateTime.now().plusMinutes(this.config.getInterval());
        } else {
            this.status = TaskStatus.INACTIVE;
            this.nextRunTime = null; // 停用时清除调度
        }
    }

    // Behavior: 推进调度 (Information Expert)
    public void advanceSchedule() {
        if (this.nextRunTime == null) {
            this.nextRunTime = LocalDateTime.now();
        }

        this.nextRunTime = LocalDateTime.now().plusMinutes(this.config.getInterval());
    }

    // Info Expert: 权限校验
    public void validateOwnership(Long operatorId) {
        if (!this.userId.equals(operatorId)) {
            throw new DomainException("Permission denied: You do not own this task.");
        }
    }

    private static void validateKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            throw new DomainException("Keywords cannot be empty");
        }
        if (keywords.size() > 10) {
            throw new DomainException("Too many keywords (max 10)");
        }
    }
}

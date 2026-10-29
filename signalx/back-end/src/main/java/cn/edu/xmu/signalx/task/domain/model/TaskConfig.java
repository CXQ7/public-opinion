package cn.edu.xmu.signalx.task.domain.model;

import cn.edu.xmu.signalx.core.exception.DomainException;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class TaskConfig {
    LocalDateTime startTime;
    LocalDateTime endTime;
    Integer interval;

    public TaskConfig(LocalDateTime startTime, LocalDateTime endTime, Integer interval) {
        // 校验逻辑：结束时间不能早于开始时间
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new DomainException("End time cannot be earlier than start time");
        }

        this.startTime = startTime;
        this.endTime = endTime;
        this.interval = interval;
    }

    // Immutable update pattern
    public TaskConfig update(LocalDateTime newStartTime, LocalDateTime newEndTime, Integer newInterval) {
        return new TaskConfig(
            newStartTime != null ? newStartTime : this.startTime,
            newEndTime != null ? newEndTime : this.endTime,
            newInterval != null ? newInterval : this.interval
        );
    }
}

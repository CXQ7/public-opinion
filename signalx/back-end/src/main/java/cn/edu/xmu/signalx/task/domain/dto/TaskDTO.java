package cn.edu.xmu.signalx.task.domain.dto;

import cn.edu.xmu.signalx.core.dto.BasePageDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

public class TaskDTO {

    @Data
    public static class Create {
        private Long userId; // Optional if inferred from context
        private List<String> keywords;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer interval;
    }

    @Data
    public static class Update {
        private List<String> keywords;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer interval;
    }

    @Data
    public static class Switch {
        private Boolean enable;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class Query extends BasePageDTO {
        private Long userId;
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class SliceQuery extends BasePageDTO {
        private Long userId;
        private Long taskId;
    }
}

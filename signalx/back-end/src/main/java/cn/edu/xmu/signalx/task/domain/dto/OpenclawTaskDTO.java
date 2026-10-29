package cn.edu.xmu.signalx.task.domain.dto;

import lombok.Data;
import java.time.LocalDateTime;

public class OpenclawTaskDTO {

    @Data
    public static class Create {
        private String name;
        private String url;
        private String crawlLogic;
        private Integer interval;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
    }

    @Data
    public static class Update {
        private String name;
        private String url;
        private String crawlLogic;
        private Integer interval;
    }

    @Data
    public static class Switch {
        private Boolean enable;
    }
}

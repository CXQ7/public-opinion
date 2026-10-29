package cn.edu.xmu.signalx.task.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class OpenclawTaskVO {
    private Long id;
    private Long userId;
    private String name;
    private String url;
    private String crawlLogic;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer interval;
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime nextRunTime;
}

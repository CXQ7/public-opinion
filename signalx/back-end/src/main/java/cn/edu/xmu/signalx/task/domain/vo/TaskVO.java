package cn.edu.xmu.signalx.task.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskVO {
    private Long id;
    private Long userId;
    private List<String> keywords;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer interval;
    private Integer status;
    private LocalDateTime createTime;
}

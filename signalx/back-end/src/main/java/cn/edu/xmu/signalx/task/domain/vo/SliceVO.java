package cn.edu.xmu.signalx.task.domain.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SliceVO {
    private Long id;
    private Long taskId;
    private String title;
    private Double heat;
    private String sentiment;
    private String summary;
    private LocalDateTime eventTime;
}

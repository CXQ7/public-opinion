package cn.edu.xmu.signalx.report.controller.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportDetailDTO {
    private Long id;
    private Long taskId;
    private String name;
    private String summary;
    private Integer status;
    private LocalDateTime createTime;
}

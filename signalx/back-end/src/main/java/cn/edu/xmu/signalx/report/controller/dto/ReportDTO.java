package cn.edu.xmu.signalx.report.controller.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReportDTO {
    private Long id;
    private Long taskId;
    private String name;
    private Integer status; // 0, 1, 2
    private LocalDateTime createTime;
}

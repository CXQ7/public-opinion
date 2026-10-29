package cn.edu.xmu.signalx.report.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("report")
public class ReportPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long taskId;
    private Integer taskType;
    private String name;

    // In schema I used longtext for summary
    private String summary;

    private String cozeRunId;
    private String fileUrl;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}

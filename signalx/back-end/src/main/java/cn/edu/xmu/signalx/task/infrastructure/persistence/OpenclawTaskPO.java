package cn.edu.xmu.signalx.task.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "openclaw_task")
public class OpenclawTaskPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String url;

    private String crawlLogic;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @TableField(value = "`interval`") // interval is a keyword in MySQL
    private Integer interval;

    private Integer status;

    private LocalDateTime nextRunTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;
}

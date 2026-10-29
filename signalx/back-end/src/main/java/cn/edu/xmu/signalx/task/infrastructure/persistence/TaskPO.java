package cn.edu.xmu.signalx.task.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName(value = "task", autoResultMap = true)
public class TaskPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> keywords;

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

package cn.edu.xmu.signalx.task.infrastructure.persistence;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("slice")
public class SlicePO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private String title;
    private Double heat;
    private String sentiment; // Enum as String
    private String summary;
    private LocalDateTime eventTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}

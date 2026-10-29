package cn.edu.xmu.signalx.record.infrastructure.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("news")
public class NewsPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Integer taskType;
    private Long sliceId;
    private String title;
    private String content;
    private String url;
    private String source;
    private String sentiment; // Stored as String
    private LocalDateTime publishTime;
    private LocalDateTime createTime;
}

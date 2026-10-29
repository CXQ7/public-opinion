package cn.edu.xmu.domain.vo;

import cn.edu.xmu.domain.po.Event;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class KeywordVO implements Serializable {
    @Schema(description = "id")
    private Long id;                    // 关键词唯一ID
    @Schema(description = "关键词")
    private String keyword;             // 关键词
    @Schema(description = "关键词对应的代表事件")
    private Event representativeEvent;  // 关键词对应的代表事件
    @Schema(description = "热度")
    private Integer heat;               // 热度
    @Schema(description = "分析")
    private String analysis;             // 分析

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;             // 更新时间

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;             // 更新时间
}

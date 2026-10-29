package cn.edu.xmu.domain.po;

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
public class Event implements Serializable {

    @Schema(description = "id")
    private Long id;               // 事件唯一ID
    @Schema(description = "关键词外键ID")
    private Long keywordId;        // 关联关键词的外键ID
    @Schema(description = "事件描述")
    private String description;    // 事件描述
    @Schema(description = "热度")
    private Double heat;    // 热度
    @Schema(description = "关联博客的ids，空格分割")
    private String newsIds;         // 关联博客的外键IDs，空格分割
    @Schema(description = "关联博客的urls，空格分割")
    private String urls;            // 事件博客的URL，空格分割
    @Schema(description = "情感值，趋0代表负面，趋1代表正面")
    private Float sentiment;      // 情感值

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Schema(description = "事件发生时间")
    private LocalDateTime eventDatetime;   // 事件发生时间

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Schema(description = "创建时间")
    private LocalDateTime createTime;    // 创建时间

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;    // 更新时间
}

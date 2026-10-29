package cn.edu.xmu.domain.doc;

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
public class EventDoc implements Serializable {

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

    @Schema(description = "事件发生时间")
    private String eventDatetime;   // 事件发生时间

    @Schema(description = "创建时间")
    private String createTime;    // 创建时间

    @Schema(description = "更新时间")
    private String updateTime;    // 更新时间
}

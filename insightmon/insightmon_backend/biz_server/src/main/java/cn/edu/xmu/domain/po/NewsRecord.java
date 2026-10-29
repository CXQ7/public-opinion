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

@Schema(description = "爬取的新闻记录")
@Data
public class NewsRecord implements Serializable {
    @Schema(description = "id")
    private Long id;               // 事件唯一ID
    @Schema(description = "来源")
    private String source;          // 话题
    @Schema(description = "作者")
    private String author;          // 话题
    @Schema(description = "话题")
    private String title;          // 话题
    @Schema(description = "内容")
    private String content;          // 话题
    @Schema(description = "热度")
    private Double heat;    // 评论数
    @Schema(description = "事件Url")
    private String url;            // 事件的URL

    @Schema(description = "新闻发布时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime publishDatetime;   // 新闻发布时间

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    private LocalDateTime createTime;    // 创建时间
}

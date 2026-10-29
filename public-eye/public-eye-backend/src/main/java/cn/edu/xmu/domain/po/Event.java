package cn.edu.xmu.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
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
    private Double sentiment;      // 情感值
    @Schema(description = "事件发生日期")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime eventDatetime;   // 事件发生日期
    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;    // 创建时间
    @Schema(description = "更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;    // 更新时间
}

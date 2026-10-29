package cn.edu.xmu.domain.po;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Keyword implements Serializable {
    @Schema(description = "id")
    private Long id;                    // 关键词唯一ID
    @Schema(description = "关键词")
    private String keyword;             // 关键词
    @Schema(description = "热度")
    private Double heat;               // 热度
    @Schema(description = "记录创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;  // 记录创建时间
    @Schema(description = "记录更新时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;  // 记录更新时间
    @Schema(description = "时间轴分析")
    private String analysis;           // 时间轴分析
}

package cn.edu.xmu.domain.doc;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
public class KeywordVODoc implements Serializable {
    @Schema(description = "id")
    private Long id;                    // 关键词唯一ID
    @Schema(description = "关键词")
    private String keyword;             // 关键词
    @Schema(description = "关键词对应的代表事件")
    private EventDoc representativeEvent;  // 关键词对应的代表事件
    @Schema(description = "热度")
    private Integer heat;               // 热度
    @Schema(description = "分析")
    private String analysis;             // 分析
    @Schema(description = "创建事件")
    private String createTime;             // 创建时间
    @Schema(description = "更新时间")
    private String updateTime;             // 更新时间
}

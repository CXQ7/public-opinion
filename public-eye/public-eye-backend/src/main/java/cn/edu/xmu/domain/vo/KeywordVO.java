package cn.edu.xmu.domain.vo;

import cn.edu.xmu.domain.po.Event;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

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
    @Schema(description = "情感")
    private Integer sentiment;          // 情感
}

package cn.edu.xmu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户询问事件链的传入对象")
@Data
public class AnalysisAskDTO {
    @Schema(description = "关键词id")
    private Long keywordId;        // 关键词id
    @Schema(description = "问题")
    private String question;       // 问题
}

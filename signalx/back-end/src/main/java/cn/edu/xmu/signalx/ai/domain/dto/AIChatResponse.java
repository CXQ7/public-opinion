package cn.edu.xmu.signalx.ai.domain.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AIChatResponse {
    private String answer;
    private String reasoningContent;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
}

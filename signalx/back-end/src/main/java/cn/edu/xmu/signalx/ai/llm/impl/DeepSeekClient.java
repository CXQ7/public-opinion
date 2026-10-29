package cn.edu.xmu.signalx.ai.llm.impl;

import cn.edu.xmu.signalx.ai.domain.dto.AIChatResponse;
import cn.edu.xmu.signalx.ai.domain.dto.DeepSeekRequest;
import cn.edu.xmu.signalx.ai.domain.dto.DeepSeekResponse;
import cn.edu.xmu.signalx.ai.llm.LLMClient;
import cn.edu.xmu.signalx.core.config.AIProperties;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeepSeekClient implements LLMClient {

    private final AIProperties aiProperties;
    private final ObjectMapper objectMapper; // Use Jackson ObjectMapper injected by Spring

    @Override
    public AIChatResponse chat(DeepSeekRequest request) {
        String url = aiProperties.getBaseUrl() + "/chat/completions";

        // Ensure request parameters
        if (request.getTemperature() == null) {
            request.setTemperature(aiProperties.getTemperature());
        }

        log.info("Sending request to DeepSeek: model={}, messages_count={}", request.getModel(), request.getMessages().size());

        try {
            // Use Jackson to serialize request to ensure @JsonProperty support if any (though Request DTO is simple)
            String requestBody = objectMapper.writeValueAsString(request);

            HttpResponse response = HttpRequest.post(url)
                    .header("Authorization", "Bearer " + aiProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .body(requestBody)
                    .timeout(120000) // Increase timeout to 120s for R1 reasoning
                    .execute();

            if (!response.isOk()) {
                log.error("DeepSeek API Error: {} - {}", response.getStatus(), response.body());
                throw new RuntimeException("AI Service Unavailable: " + response.getStatus() + " " + response.body());
            }

            String responseBody = response.body();
            log.info("DeepSeek Raw Response: {}", responseBody); // Temporary logging for debug
            // Use Jackson to deserialize response to support @JsonProperty
            DeepSeekResponse deepSeekResponse = objectMapper.readValue(responseBody, DeepSeekResponse.class);

            if (deepSeekResponse.getChoices() == null || deepSeekResponse.getChoices().isEmpty()) {
                throw new RuntimeException("Empty response from AI");
            }

            DeepSeekResponse.Choice choice = deepSeekResponse.getChoices().get(0);
            DeepSeekResponse.Usage usage = deepSeekResponse.getUsage();

            return AIChatResponse.builder()
                    .answer(choice.getMessage().getContent())
                    .reasoningContent(choice.getMessage().getReasoningContent())
                    .promptTokens(usage != null ? usage.getPromptTokens() : 0)
                    .completionTokens(usage != null ? usage.getCompletionTokens() : 0)
                    .totalTokens(usage != null ? usage.getTotalTokens() : 0)
                    .build();

        } catch (Exception e) {
            log.error("Failed to communicate with DeepSeek", e);
            throw new RuntimeException("AI Interaction Failed", e);
        }
    }
}

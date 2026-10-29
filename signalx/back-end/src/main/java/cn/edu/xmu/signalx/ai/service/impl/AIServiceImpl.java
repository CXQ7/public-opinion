package cn.edu.xmu.signalx.ai.service.impl;

import cn.edu.xmu.signalx.ai.domain.dto.AIChatResponse;
import cn.edu.xmu.signalx.ai.domain.dto.DeepSeekRequest;
import cn.edu.xmu.signalx.ai.domain.entity.QALog;
import cn.edu.xmu.signalx.ai.enums.AITargetType;
import cn.edu.xmu.signalx.ai.llm.LLMClient;
import cn.edu.xmu.signalx.ai.infrastructure.mapper.QALogMapper;
import cn.edu.xmu.signalx.ai.service.AIService;
import cn.edu.xmu.signalx.ai.strategy.QAContextStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AIServiceImpl implements AIService {

    private final List<QAContextStrategy> strategies;
    private final LLMClient llmClient;
    private final QALogMapper qaLogMapper;

    private Map<AITargetType, QAContextStrategy> strategyMap;

    @PostConstruct
    public void init() {
        strategyMap = new HashMap<>();
        for (QAContextStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
        }
    }

    @Override
    public AIChatResponse askReport(Long userId, Long reportId, String question) {
        return handleRequest(userId, reportId, AITargetType.REPORT, question);
    }

    @Override
    public AIChatResponse askEvent(Long userId, Long eventId, String question) {
        return handleRequest(userId, eventId, AITargetType.EVENT, question);
    }

    private AIChatResponse handleRequest(Long userId, Long targetId, AITargetType type, String question) {
        QAContextStrategy strategy = strategyMap.get(type);
        if (strategy == null) {
            throw new UnsupportedOperationException("No strategy found for type: " + type);
        }

        // 1. Build System Prompt (Static Context)
        String systemPrompt = strategy.buildSystemPrompt(targetId);
        String model = strategy.getPreferredModel();

        // 2. Fetch Conversation History (Dynamic Context)
        List<DeepSeekRequest.Message> historyMessages = fetchHistory(userId, targetId, type.getCode());

        // 3. Assemble Messages
        List<DeepSeekRequest.Message> messages = new ArrayList<>();
        // System Prompt
        messages.add(DeepSeekRequest.Message.builder()
                .role("system")
                .content(systemPrompt)
                .build());

        // History
        messages.addAll(historyMessages);

        // Current Question
        messages.add(DeepSeekRequest.Message.builder()
                .role("user")
                .content(question)
                .build());

        // 4. Call LLM
        DeepSeekRequest request = DeepSeekRequest.builder()
                .model(model)
                .messages(messages)
                .stream(false)
                .build();

        AIChatResponse response = llmClient.chat(request);

        // 5. Save Log
        saveLog(userId, targetId, type.getCode(), model, question, response);

        return response;
    }

    private List<DeepSeekRequest.Message> fetchHistory(Long userId, Long targetId, Integer targetType) {
        // Fetch top 10 logs (latest first)
        List<QALog> logs = qaLogMapper.selectList(new LambdaQueryWrapper<QALog>()
                .eq(QALog::getUserId, userId)
                .eq(QALog::getTargetId, targetId)
                .eq(QALog::getTargetType, targetType)
                .orderByDesc(QALog::getCreateTime)
                .last("LIMIT 10"));

        // Reverse to chronological order (oldest first)
        Collections.reverse(logs);

        List<DeepSeekRequest.Message> messages = new ArrayList<>();
        for (QALog logEntry : logs) {
            // User Message
            if (logEntry.getQuestion() != null) {
                messages.add(DeepSeekRequest.Message.builder()
                        .role("user")
                        .content(logEntry.getQuestion())
                        .build());
            }
            // Assistant Message (Exclude reasoning content for history)
            if (logEntry.getAnswer() != null) {
                messages.add(DeepSeekRequest.Message.builder()
                        .role("assistant")
                        .content(logEntry.getAnswer())
                        .build());
            }
        }
        return messages;
    }

    private void saveLog(Long userId, Long targetId, Integer targetType, String model, String question, AIChatResponse response) {
        QALog qaLog = QALog.builder()
                .userId(userId)
                .targetId(targetId)
                .targetType(targetType)
                .model(model)
                .question(question)
                .answer(response.getAnswer())
                .reasoningContent(response.getReasoningContent())
                .promptTokens(response.getPromptTokens())
                .completionTokens(response.getCompletionTokens())
                .totalTokens(response.getTotalTokens())
                .createTime(java.time.LocalDateTime.now())
                .build();
        qaLogMapper.insert(qaLog);
    }
}

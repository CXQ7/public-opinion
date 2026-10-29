package cn.edu.xmu.signalx.ai.service;

import cn.edu.xmu.signalx.ai.domain.dto.AIChatResponse;
import cn.edu.xmu.signalx.ai.domain.dto.DeepSeekRequest;
import cn.edu.xmu.signalx.ai.domain.entity.QALog;
import cn.edu.xmu.signalx.ai.enums.AITargetType;
import cn.edu.xmu.signalx.ai.llm.LLMClient;
import cn.edu.xmu.signalx.ai.infrastructure.mapper.QALogMapper;
import cn.edu.xmu.signalx.ai.service.impl.AIServiceImpl;
import cn.edu.xmu.signalx.ai.strategy.QAContextStrategy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AIServiceTest {

    @Mock
    private LLMClient llmClient;

    @Mock
    private QALogMapper qaLogMapper;

    @Mock
    private QAContextStrategy reportStrategy;

    @Mock
    private QAContextStrategy eventStrategy;

    private AIServiceImpl aiService;

    @BeforeEach
    void setUp() {
        // Setup Strategy Mocks
        lenient().when(reportStrategy.getType()).thenReturn(AITargetType.REPORT);
        lenient().when(eventStrategy.getType()).thenReturn(AITargetType.EVENT);

        lenient().when(reportStrategy.getPreferredModel()).thenReturn("deepseek-reasoner");
        lenient().when(eventStrategy.getPreferredModel()).thenReturn("deepseek-chat");

        List<QAContextStrategy> strategies = Arrays.asList(reportStrategy, eventStrategy);
        aiService = new AIServiceImpl(strategies, llmClient, qaLogMapper);
        aiService.init(); // Manually trigger @PostConstruct
    }

    @Test
    void testAskReport_Success() {
        Long userId = 1L;
        Long reportId = 100L;
        String question = "Analyze this";
        String systemPrompt = "Report Content";

        // Mock Strategy
        when(reportStrategy.buildSystemPrompt(reportId)).thenReturn(systemPrompt);

        // Mock History (Empty)
        when(qaLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

        // Mock LLM Response
        AIChatResponse mockResponse = AIChatResponse.builder()
                .answer("Analysis result")
                .reasoningContent("Thinking process")
                .promptTokens(10)
                .completionTokens(20)
                .totalTokens(30)
                .build();
        when(llmClient.chat(any(DeepSeekRequest.class))).thenReturn(mockResponse);

        // Execute
        AIChatResponse result = aiService.askReport(userId, reportId, question);

        // Verify
        assertEquals("Analysis result", result.getAnswer());
        assertEquals("Thinking process", result.getReasoningContent());

        // Verify LLM Request
        ArgumentCaptor<DeepSeekRequest> requestCaptor = ArgumentCaptor.forClass(DeepSeekRequest.class);
        verify(llmClient).chat(requestCaptor.capture());
        DeepSeekRequest capturedRequest = requestCaptor.getValue();

        assertEquals("deepseek-reasoner", capturedRequest.getModel());
        assertEquals(2, capturedRequest.getMessages().size());
        assertEquals("system", capturedRequest.getMessages().get(0).getRole());
        assertEquals(systemPrompt, capturedRequest.getMessages().get(0).getContent());
        assertEquals("user", capturedRequest.getMessages().get(1).getRole());
        assertEquals(question, capturedRequest.getMessages().get(1).getContent());

        // Verify Log Saving
        verify(qaLogMapper).insert(any(QALog.class));
    }

    @Test
    void testAskEvent_WithHistory() {
        Long userId = 1L;
        Long eventId = 200L;
        String question = "Follow up question";
        String systemPrompt = "Event Info";

        // Mock Strategy
        when(eventStrategy.buildSystemPrompt(eventId)).thenReturn(systemPrompt);

        // Mock History
        QALog historyLog1 = new QALog();
        historyLog1.setQuestion("Q1");
        historyLog1.setAnswer("A1");
        historyLog1.setReasoningContent("Reasoning1"); // Should be filtered out

        when(qaLogMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(historyLog1));

        // Mock LLM Response
        AIChatResponse mockResponse = AIChatResponse.builder()
                .answer("A2")
                .build();
        when(llmClient.chat(any(DeepSeekRequest.class))).thenReturn(mockResponse);

        // Execute
        aiService.askEvent(userId, eventId, question);

        // Verify LLM Request Messages
        ArgumentCaptor<DeepSeekRequest> requestCaptor = ArgumentCaptor.forClass(DeepSeekRequest.class);
        verify(llmClient).chat(requestCaptor.capture());
        List<DeepSeekRequest.Message> messages = requestCaptor.getValue().getMessages();

        // Expected: System + User(Q1) + Assistant(A1) + User(Current)
        assertEquals(4, messages.size());

        // Check History Filtering
        DeepSeekRequest.Message historyAssistantMsg = messages.get(2);
        assertEquals("assistant", historyAssistantMsg.getRole());
        assertEquals("A1", historyAssistantMsg.getContent());
        // Note: DTO doesn't have reasoningContent field for request.Message, so it's naturally filtered by using the DTO.
        // But we should ensure logic didn't put it in content somehow.
    }
}

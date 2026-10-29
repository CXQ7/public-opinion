package cn.edu.xmu.service.keyword_service_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.edu.xmu.domain.po.Keyword;
import cn.edu.xmu.service.impl.KeywordServiceImpl;
import cn.edu.xmu.service.AIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
public class TestAskAboutAnalysis {
    @Mock
    private AIService aiService;

    @InjectMocks
    private KeywordServiceImpl keywordService;

    static class TestCase {
        final String desc;
        String analysisContent;
        String question;
        String mockResponse;
        String expected;
        String expectedExMsg;

        TestCase(String desc) {
            this.desc = desc;
            this.analysisContent = "预存分析内容";
            this.question = "测试问题";
            this.mockResponse = "AI生成回答";
            this.expected = "AI生成回答";
        }

        TestCase withEmptyAnalysis() {
            this.analysisContent = "";
            return this;
        }

        TestCase withAIResponse(String response) {
            this.mockResponse = response;
            this.expected = response;
            return this;
        }

        TestCase expectException(String msg) {
            this.expectedExMsg = msg;
            return this;
        }

        @Override
        public String toString() {
            return desc;
        }
    }

    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(new TestCase("正常提问").withAIResponse("AI生成回答")),
                Arguments.of(new TestCase("空分析内容").withEmptyAnalysis()),
                Arguments.of(new TestCase("AI服务超时").expectException("AI服务响应超时"))
        );
    }

    @BeforeEach
    public void setUp() {
        // 无需手动初始化Mockito，由MockitoExtension处理
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    public void testAskAboutAnalysis(TestCase testCase) {
        Keyword keyword = new Keyword();
        keyword.setAnalysis(testCase.analysisContent);

        // 配置AI响应
        if (testCase.desc.equals("AI服务超时")) {
            when(aiService.ask(anyString())).thenThrow(new RuntimeException("AI服务响应超时"));
        } else {
            when(aiService.ask(anyString())).thenReturn(testCase.mockResponse);
        }

        if (testCase.expectedExMsg != null) {
            // 测试异常情况
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                keywordService.askAboutAnalysis(keyword, testCase.question);
            });
            assertTrue(exception.getMessage().contains(testCase.expectedExMsg), "异常消息应包含: " + testCase.expectedExMsg);
        } else {
            // 测试正常情况
            String result = keywordService.askAboutAnalysis(keyword, testCase.question);
            assertEquals(testCase.expected, result);

            // 验证问题格式
            verify(aiService).ask(argThat(query ->
                    query.contains("已知事件链") &&
                            query.contains(testCase.question)
            ));
        }
    }
}

package cn.edu.xmu.service.keyword_service_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.edu.xmu.domain.po.Keyword;
import cn.edu.xmu.mapper.KeywordMapper;
import cn.edu.xmu.service.impl.KeywordServiceImpl;
import cn.edu.xmu.service.AIService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
// notification: 用例由AI生成, 未经人工校验
public class TestAnalyze {
    @Mock
    private KeywordMapper keywordMapper;
    @Mock
    private AIService aiService;

    @Spy
    @InjectMocks
    private KeywordServiceImpl keywordService;

    static class TestCase {
        final String desc;
        String existingAnalysis;
        String mockResponse;
        String expected;
        String expectedExMsg;

        TestCase(String desc) {
            this.desc = desc;
            this.existingAnalysis = null;
            this.mockResponse = "生成的分析内容";
            this.expected = "生成的分析内容";
        }

        TestCase withExistingAnalysis(String analysis) {
            this.existingAnalysis = analysis;
            return this;
        }

        TestCase expectResult(String result) {
            this.expected = result;
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
                Arguments.of(new TestCase("已有分析直接返回")
                        .withExistingAnalysis("预存分析")
                        .expectResult("预存分析")),
                Arguments.of(new TestCase("生成新分析")
                        .expectResult("生成的分析内容")),
                Arguments.of(new TestCase("AI服务超时")
                        .expectException("AI服务请求超时"))
        );
    }

    @BeforeEach
    public void setUp() {
        // 无需手动初始化Mockito
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    public void testAnalyze(TestCase testCase) {
        Keyword keyword = new Keyword();
        keyword.setAnalysis(testCase.existingAnalysis);

        // 配置AI响应
        if (testCase.desc.equals("AI服务超时")) {
            when(aiService.ask(anyString())).thenThrow(new RuntimeException("AI服务请求超时"));
            doReturn("模拟时间链").when(keywordService).getTimeChainStr(any());;
        } else if (testCase.desc.equals("生成新分析")) {
            when(aiService.ask(anyString())).thenReturn(testCase.mockResponse);
            doReturn("模拟时间链").when(keywordService).getTimeChainStr(any());;
        }

        if (testCase.expectedExMsg != null) {
            // 测试异常情况
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                keywordService.analyze(keyword);
            });
            assertTrue(exception.getMessage().startsWith(testCase.expectedExMsg)
                    ,"异常消息应以指定内容开头: " + exception.getMessage());
        } else {
            // 测试正常情况
            String result = keywordService.analyze(keyword);
            assertEquals(testCase.expected, result);

            // 验证数据库更新
            if (testCase.existingAnalysis == null) {
                verify(keywordMapper).updateById(argThat(k ->
                        k.getAnalysis().equals(testCase.expected)
                ));
            } else {
                verify(keywordMapper, never()).updateById(any());
            }
        }
    }
}

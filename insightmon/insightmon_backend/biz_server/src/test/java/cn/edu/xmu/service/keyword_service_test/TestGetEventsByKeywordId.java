package cn.edu.xmu.service.keyword_service_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.mapper.EventMapper;
import cn.edu.xmu.service.impl.KeywordServiceImpl;
import cn.edu.xmu.utils.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
// notification: 用例由AI生成, 未经人工校验
public class TestGetEventsByKeywordId {
    @Mock
    private EventMapper eventMapper;
    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private KeywordServiceImpl keywordService;

    private static Event event(Long id, String desc) {
        Event e = new Event();
        e.setId(id);
        e.setDescription(desc);
        return e;
    }

    private static final List<Event> MOCK_EVENTS = Arrays.asList(
            event(1L, "测试事件1"),
            event(2L, "测试事件2")
    );

    static class TestCase {
        final String desc;
        final Long keywordId;
        List<Event> expected;
        String expectedExMsg;

        TestCase(String desc) {
            this.desc = desc;
            this.keywordId = 1L;
            this.expected = Collections.emptyList();
        }

        TestCase expectEvents(List<Event> events) {
            this.expected = events;
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
                Arguments.of(new TestCase("缓存命中").expectEvents(MOCK_EVENTS)),
                Arguments.of(new TestCase("数据库查询").expectEvents(MOCK_EVENTS)),
                Arguments.of(new TestCase("空结果集").expectEvents(Collections.emptyList())),
                Arguments.of(new TestCase("数据库异常").expectException("数据库连接失败"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    public void testGetEventsByKeywordId(TestCase testCase) {
        lenient().when(eventMapper.getEventsByKeywordId(anyLong())).thenReturn(
                testCase.desc.equals("数据库异常") ? null : testCase.expected
        );
        if (testCase.desc.equals("缓存命中")) {
            when(redisUtil.get(anyString())).thenReturn(testCase.expected);
        } else {
            // 非缓存命中情况，确保redisUtil.get返回null
            when(redisUtil.get(anyString())).thenReturn(null);
        }

        // 配置数据库异常
        if (testCase.desc.equals("数据库异常")) {
            when(eventMapper.getEventsByKeywordId(anyLong()))
                    .thenThrow(new RuntimeException("数据库连接失败"));
        }

        if (testCase.expectedExMsg != null) {
            // 测试异常情况
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                keywordService.getEventsByKeywordId(testCase.keywordId);
            });
            assertTrue(exception.getMessage().startsWith(testCase.expectedExMsg),
                    "异常消息应以指定内容开头: " + exception.getMessage());
        } else {
            // 测试正常情况
            List<Event> result = keywordService.getEventsByKeywordId(testCase.keywordId);
            assertEquals(testCase.expected, result);

            // 验证缓存存储
            if (!testCase.desc.equals("缓存命中") && !testCase.desc.equals("数据库异常")) {
                verify(redisUtil).set(anyString(), eq(testCase.expected));
            }
        }
    }
}

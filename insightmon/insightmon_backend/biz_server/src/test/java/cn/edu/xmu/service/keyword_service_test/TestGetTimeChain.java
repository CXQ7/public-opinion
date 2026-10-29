package cn.edu.xmu.service.keyword_service_test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.domain.po.Keyword;
import cn.edu.xmu.domain.po.NewsRecord;
import cn.edu.xmu.mapper.EventMapper;
import cn.edu.xmu.mapper.NewsRecordMapper;
import cn.edu.xmu.service.impl.KeywordServiceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
// notification: 用例已人工校验
public class TestGetTimeChain {
    @Mock
    private EventMapper eventMapper;

    @Mock
    private NewsRecordMapper newsRecordMapper;

    @InjectMocks
    private KeywordServiceImpl keywordService;

    private static NewsRecord news(Long id, String title) {
        NewsRecord n = new NewsRecord();
        n.setId(id);
        n.setTitle(title);
        return n;
    }

    private static Event event(String desc, String datetimeStr, NewsRecord... newsList) {
        Event e = new Event();
        e.setDescription(desc);
        e.setNewsIds(Arrays.stream(newsList)
                .map(n -> n.getId().toString())
                .collect(Collectors.joining(" ")));
        e.setEventDatetime(LocalDateTime.parse(datetimeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        return e;
    }

    private static final List<NewsRecord> RECORD_LIST = Arrays.asList(
            news(101L, ""),
            news(102L, "新闻标题2"),
            news(103L, "新闻标题3")
    );

    private static final List<Event> EVENT_LIST = Arrays.asList(
            event("无news的event",  "2023-01-01T00:00:00"),
            event("news无标题的event", "2023-01-02T00:00:00", RECORD_LIST.get(0)),
            event("1个news的event", "2023-01-02T00:00:00", RECORD_LIST.get(1)),
            event("2个news的event", "2023-01-02T00:00:00", RECORD_LIST.get(1), RECORD_LIST.get(2)),
            event("3个news的event", "2023-01-02T00:00:00", RECORD_LIST.get(1), RECORD_LIST.get(2))
    );

    @BeforeEach
    public void setUp() {
        // 初始化新闻记录的模拟
        for (Event event : EVENT_LIST) {
            List<NewsRecord> newsRecords = new ArrayList<>();
            for (String newsId : parseNewsIds(event.getNewsIds())) {
                if (newsId.isEmpty()) {
                    continue;
                }
                Long newsIdNum = Long.parseLong(newsId);
                for (NewsRecord news : RECORD_LIST) {
                    if (news.getId().equals(newsIdNum)) {
                        newsRecords.add(news);
                        break;
                    }
                }
            }
            lenient().when(newsRecordMapper.selectBatchIds(parseNewsIds(event.getNewsIds()))).thenReturn(newsRecords);
        }
    }

    static Stream<Arguments> testCases() {
        return Stream.of(
                Arguments.of(new TestCase("无事件")
                        .withMockEvents(Collections.emptyList())
                        .expectResult("")),
                Arguments.of(new TestCase("无News")
                        .withMockEvents(Arrays.asList(EVENT_LIST.get(0)))
                        .expectExceptionMsg("No news records found for event")),
                Arguments.of(new TestCase("new的标题是空")
                        .withMockEvents(Arrays.asList(EVENT_LIST.get(1)))
                        .expectResult("2023-01-02T00:00: ")),
                Arguments.of(new TestCase("单事件单新闻")
                        .withMockEvents(Arrays.asList(EVENT_LIST.get(2)))
                        .expectResult("2023-01-02T00:00: 新闻标题2")),
                Arguments.of(new TestCase("多事件多新闻")
                        .withMockEvents(Arrays.asList(EVENT_LIST.get(3), EVENT_LIST.get(4)))
                        .expectResult("2023-01-02T00:00: 新闻标题2 & 新闻标题3\\n|2023-01-02T00:00: 新闻标题2 & 新闻标题3"))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testCases")
    public void testGetTimeChainStr(TestCase testCase) {
        // Mock事件查询
        when(eventMapper.selectList(any())).thenReturn(testCase.events);

        Keyword keyword = new Keyword();
        keyword.setId(testCase.keywordId);

        if (testCase.expectedExceptionMsg != null && !testCase.expectedExceptionMsg.isEmpty()) {
            // 测试异常情况
            Exception exception = assertThrows(Exception.class, () -> {
                keywordService.getTimeChainStr(keyword);
            });
            assertTrue(exception.getMessage().contains(testCase.expectedExceptionMsg)
            , "Exception message should contain: " + testCase.expectedExceptionMsg +
                            ", but was: " + exception.getMessage());
        } else {
            // 测试正常情况
            String result = keywordService.getTimeChainStr(keyword);
            assertEquals(testCase.expectedResult, result);
        }
    }

    // 辅助方法
    private static List<String> parseNewsIds(String ids) {
        return List.of(ids.split(" "));
    }

    static class TestCase {
        String description;
        Long keywordId;
        List<Event> events = new ArrayList<>();
        String expectedResult;
        String expectedExceptionMsg;

        TestCase(String description) {
            this.description = description;
            this.keywordId = 1L;
        }

        TestCase withMockEvents(List<Event> events) {
            this.events = events;
            return this;
        }

        TestCase expectResult(String result) {
            this.expectedResult = result;
            return this;
        }

        TestCase expectExceptionMsg(String msg) {
            this.expectedExceptionMsg = msg;
            return this;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}

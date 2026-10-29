package cn.edu.xmu.signalx.ai.strategy;

import cn.edu.xmu.signalx.ai.strategy.impl.EventQAStrategy;
import cn.edu.xmu.signalx.record.infrastructure.mapper.NewsMapper;
import cn.edu.xmu.signalx.record.infrastructure.po.NewsPO;
import cn.edu.xmu.signalx.task.infrastructure.mapper.SliceMapper;
import cn.edu.xmu.signalx.task.infrastructure.persistence.SlicePO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventQAStrategyTest {

    @Mock
    private SliceMapper sliceMapper;

    @Mock
    private NewsMapper newsMapper;

    @InjectMocks
    private EventQAStrategy eventQAStrategy;

    @Test
    void testBuildSystemPrompt_Success() {
        Long eventId = 1L;

        // Mock Slice
        SlicePO mockSlice = new SlicePO();
        mockSlice.setId(eventId);
        mockSlice.setTitle("Test Event");
        mockSlice.setSummary("Event Summary");
        mockSlice.setSentiment("NEGATIVE");
        mockSlice.setEventTime(LocalDateTime.now());
        when(sliceMapper.selectById(eventId)).thenReturn(mockSlice);

        // Mock News
        NewsPO news1 = new NewsPO();
        news1.setTitle("News 1");
        news1.setSource("Source A");
        news1.setPublishTime(LocalDateTime.now());
        news1.setContent("Content 1");

        NewsPO news2 = new NewsPO();
        news2.setTitle("News 2");
        news2.setSource("Source B");
        news2.setPublishTime(LocalDateTime.now());
        news2.setContent(null); // Test null content

        List<NewsPO> mockNewsList = Arrays.asList(news1, news2);
        when(newsMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(mockNewsList);

        // Execute
        String prompt = eventQAStrategy.buildSystemPrompt(eventId);

        // Verify
        assertNotNull(prompt);
        assertTrue(prompt.contains("Test Event"));
        assertTrue(prompt.contains("NEGATIVE"));
        assertTrue(prompt.contains("News 1"));
        assertTrue(prompt.contains("Source A"));
        assertTrue(prompt.contains("News 2"));
    }

    @Test
    void testGetPreferredModel() {
        assertEquals("deepseek-chat", eventQAStrategy.getPreferredModel());
    }
}

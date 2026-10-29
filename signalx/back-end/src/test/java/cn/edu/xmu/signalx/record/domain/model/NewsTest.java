package cn.edu.xmu.signalx.record.domain.model;

import cn.edu.xmu.signalx.core.exception.DomainException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

public class NewsTest {

    @Test
    public void testCreateNews_Success() {
        Long taskId = 1L;
        String title = "Test News";
        String content = "Content";
        String url = "http://example.com";
        String source = "Weibo";

        // Updated: added taskType=0
        News news = News.create(taskId, 0, title, content, url, source, null);

        Assertions.assertNotNull(news);
        Assertions.assertEquals(taskId, news.getTaskId());
        Assertions.assertEquals(title, news.getTitle());
        Assertions.assertEquals(url, news.getUrl());
        Assertions.assertEquals("NEUTRAL", news.getSentiment()); // Default
        Assertions.assertNotNull(news.getPublishTime());
    }

    @Test
    public void testCreateNews_ValidationFail() {
        // Test Null Task ID
        Assertions.assertThrows(DomainException.class, () -> {
            News.create(null, 0, "Title", "Content", "url", "source", null);
        });

        // Test Empty Title
        Assertions.assertThrows(DomainException.class, () -> {
            News.create(1L, 0, "", "Content", "url", "source", null);
        });

        // Test Empty URL
        Assertions.assertThrows(DomainException.class, () -> {
            News.create(1L, 0, "Title", "Content", "", "source", null);
        });
    }

    @Test
    public void testIsHighRisk() {
        // Reconstruct a NEGATIVE news - added taskType=0
        News negativeNews = News.reconstruct(1L, 1L, 0, 1L, "Title", "Content", "url", "source", LocalDateTime.now(), "NEGATIVE");
        Assertions.assertTrue(negativeNews.isHighRisk());

        // Reconstruct a POSITIVE news - added taskType=0
        News positiveNews = News.reconstruct(2L, 1L, 0, 1L, "Title", "Content", "url", "source", LocalDateTime.now(), "POSITIVE");
        Assertions.assertFalse(positiveNews.isHighRisk());
    }
}

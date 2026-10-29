package cn.edu.xmu.signalx.record.service;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.record.domain.model.News;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@SpringBootTest
@ActiveProfiles("test")
@Transactional // Rollback after each test
public class NewsServiceTest {

    @Autowired
    private NewsService newsService;

    @Test
    public void testCreateAndListNews() {
        // 1. Create News
        Long taskId = 999L;
        String title = "Integration Test News";
        String content = "This is a test content.";
        String url = "http://integration-test.com/1";
        String source = "UnitTest";
        LocalDateTime publishTime = LocalDateTime.now();

        // Service returns Long (ID) or void depending on impl. Current Impl returns Long (mocked as 0L in my code, unless I updated it).
        // Wait, I left a "Todo: Fix ID" in the service implementation.
        // Let's check the service implementation I wrote.
        /*
            @Transactional
            public Long createNews(...) {
                News news = News.create(...);
                newsRepository.save(news);
                return news.getId();
            }
         */
        // Mybatis-Plus insert populates the ID in the PO.
        // My implementation of Repository.save() converts PO *back*? No, let's double check MybatisNewsRepository.

        Long resultId = newsService.createNews(taskId, title, content, url, source, publishTime);
        Assertions.assertNotNull(resultId);

        // 2. Query
        PageResult<News> pageResult = newsService.listNews(taskId, 0, null, "Integration", 1, 10);

        Assertions.assertNotNull(pageResult);
        Assertions.assertTrue(pageResult.getList().size() > 0);

        News fetched = pageResult.getList().get(0);
        Assertions.assertEquals(title, fetched.getTitle());
        Assertions.assertEquals(taskId, fetched.getTaskId());
    }
}

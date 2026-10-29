package cn.edu.xmu.signalx.record.service;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.redis.queue.QueueMessage;
import cn.edu.xmu.signalx.core.redis.queue.RedisQueue;
import cn.edu.xmu.signalx.record.domain.model.News;
import cn.edu.xmu.signalx.record.domain.repository.NewsRepository;
import cn.edu.xmu.signalx.record.port.CrawlerRunner;
import cn.edu.xmu.signalx.task.domain.model.Task;
import cn.edu.xmu.signalx.task.domain.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final TaskRepository taskRepository;
    private final CrawlerRunner crawlerRunner;
    private final RedisQueue redisQueue;

    public PageResult<News> listNews(Long taskId, Integer taskType, Long sliceId, String keyword, int page, int size) {
        return newsRepository.findByCondition(taskId, taskType, sliceId, keyword, page, size);
    }

    /**
     * Periodically scan active tasks and trigger their crawlers.
     */
    public void executeActiveCrawlers() {
        List<Task> activeTasks = taskRepository.findActiveTasks();
        if (activeTasks.isEmpty()) {
            return;
        }

        for (Task task : activeTasks) {
            QueueMessage<Long> msg = QueueMessage.create("news.crawl", task.getId());
            redisQueue.push(msg);
            log.info("Pushed crawler task to queue | taskId: {}, msgId: {}", task.getId(), msg.getMessageId());
        }
    }

    /**
     * Create/Save a news item.
     * Often used by internal crawlers.
     */
    @Transactional
    public Long createNews(Long taskId, String title, String content, String url, String source, LocalDateTime publishTime) {
        return createNewsWithType(taskId, 0, title, content, url, source, publishTime);
    }

    @Transactional
    public Long createNewsWithType(Long taskId, Integer taskType, String title, String content, String url, String source, LocalDateTime publishTime) {
        News news = News.create(taskId, taskType, title, content, url, source, publishTime);
        // Optional: Perform sentiment analysis here or via event bus
        // news.analyzeSentiment();
        newsRepository.save(news);
        return news.getId();
    }
}

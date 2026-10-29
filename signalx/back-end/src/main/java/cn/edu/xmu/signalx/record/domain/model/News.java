package cn.edu.xmu.signalx.record.domain.model;

import cn.edu.xmu.signalx.core.exception.DomainException;
import cn.hutool.core.util.StrUtil;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * News Entity (Rich Domain Model)
 * Information Expert for News validation and business logic.
 */
@Getter
public class News {
    @lombok.Setter
    private Long id;
    private Long taskId;
    private Integer taskType; // 0: MONITORING, 1: OPENCLAW
    private Long sliceId;
    private String title;
    private String content;
    private String url;
    private String source;
    private LocalDateTime publishTime;
    private String sentiment; // "POSITIVE", "NEGATIVE", "NEUTRAL"

    // Private constructor to enforce factory usage
    private News() {}

    /**
     * Factory method to create a valid News entity.
     * Enforces invariants.
     */
    public static News create(Long taskId, Integer taskType, String title, String content, String url, String source, LocalDateTime publishTime) {
        if (taskId == null) {
            throw new DomainException("Task ID cannot be null");
        }
        if (taskType == null) {
            taskType = 0; // Default to MONITORING
        }
        if (StrUtil.isBlank(title)) {
            throw new DomainException("News title cannot be empty");
        }
        if (StrUtil.isBlank(url)) {
            throw new DomainException("News URL cannot be empty");
        }

        News news = new News();
        news.taskId = taskId;
        news.taskType = taskType;
        news.title = title;
        news.content = content; // Content can be null/empty
        news.url = url;
        news.source = StrUtil.isBlank(source) ? "Unknown" : source;
        news.publishTime = publishTime != null ? publishTime : LocalDateTime.now();
        news.sentiment = "NEUTRAL"; // Default
        return news;
    }

    /**
     * Reconstruct from persistence.
     */
    public static News reconstruct(Long id, Long taskId, Integer taskType, Long sliceId, String title, String content,
                                 String url, String source, LocalDateTime publishTime, String sentiment) {
        News news = new News();
        news.id = id;
        news.taskId = taskId;
        news.taskType = taskType != null ? taskType : 0;
        news.sliceId = sliceId;
        news.title = title;
        news.content = content;
        news.url = url;
        news.source = source;
        news.publishTime = publishTime;
        news.sentiment = sentiment;
        return news;
    }

    /**
     * Business Logic: Check if this news is high risk (Negative).
     */
    public boolean isHighRisk() {
        return "NEGATIVE".equalsIgnoreCase(this.sentiment);
    }
}

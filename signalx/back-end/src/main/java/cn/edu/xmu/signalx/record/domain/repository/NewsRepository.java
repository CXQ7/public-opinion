package cn.edu.xmu.signalx.record.domain.repository;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.record.domain.model.News;

import java.util.List;

public interface NewsRepository {
    /**
     * Save a news entity (Create or Update).
     */
    void save(News news);

    /**
     * Find news by ID.
     */
    News findById(Long id);

    /**
     * Query news with pagination and filters.
     * All parameters are optional filter conditions (User-driven query).
     */
    PageResult<News> findByCondition(Long taskId, Integer taskType, Long sliceId, String keyword, int page, int size);
}

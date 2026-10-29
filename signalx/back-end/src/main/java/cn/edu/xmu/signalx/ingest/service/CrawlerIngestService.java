package cn.edu.xmu.signalx.ingest.service;

import cn.edu.xmu.signalx.ingest.domain.model.NewsIngestItem;
import cn.edu.xmu.signalx.ingest.domain.model.IngestResult;
import cn.edu.xmu.signalx.record.domain.model.News;
import cn.edu.xmu.signalx.record.infrastructure.converter.NewsConverter;
import cn.edu.xmu.signalx.record.infrastructure.mapper.NewsMapper;
import cn.edu.xmu.signalx.record.infrastructure.po.NewsPO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CrawlerIngestService {

    private final NewsMapper newsMapper;

    @Transactional(rollbackFor = Exception.class)
    public IngestResult batchUpsert(List<NewsIngestItem> items) {
        if (items == null || items.isEmpty()) {
            return new IngestResult(0, 0, 0);
        }

        int inserted = 0;
        int skipped = 0;

        for (NewsIngestItem item : items) {
            if (item == null || item.taskId() == null || item.url() == null || item.url().isBlank()) {
                skipped++;
                continue;
            }

            LambdaQueryWrapper<NewsPO> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(NewsPO::getTaskId, item.taskId())
                    .eq(NewsPO::getUrl, item.url())
                    .last("LIMIT 1");

            NewsPO existing = newsMapper.selectOne(wrapper);
            if (existing != null) {
                skipped++;
                continue;
            }

            LocalDateTime publishTime = parseTime(item.publishTime());
            // 使用 taskType=0 (固定为 Standard 爬虫任务)
            News news = News.create(item.taskId(), 0, item.title(), item.content(), item.url(), item.source(), publishTime);
            NewsPO po = NewsConverter.toPO(news);
            newsMapper.insert(po);
            inserted++;
        }

        return new IngestResult(items.size(), inserted, skipped);
    }

    private LocalDateTime parseTime(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(raw);
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(raw, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }
}

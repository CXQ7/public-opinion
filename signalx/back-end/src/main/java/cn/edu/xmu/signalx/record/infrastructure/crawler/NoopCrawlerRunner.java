package cn.edu.xmu.signalx.record.infrastructure.crawler;

import cn.edu.xmu.signalx.record.port.CrawlerRunner;
import cn.edu.xmu.signalx.task.domain.model.Task;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation: no-op.
 *
 * Keeps scheduling workflow complete without coupling to an actual crawler.
 */
@Slf4j
public class NoopCrawlerRunner implements CrawlerRunner {
    @Override
    public void run(Task task) {
        log.info("Crawler runner is not configured; skip crawling for task [{}]", task.getId());
    }
}
//无爬虫时进行临时事件链整合

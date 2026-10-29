package cn.edu.xmu.signalx.record.port;

import cn.edu.xmu.signalx.task.domain.model.Task;

/**
 * Pluggable crawler runner.
 *
 * Implementations can fetch and persist news for a given task.
 */
public interface CrawlerRunner {
    void run(Task task);
}
//爬虫执行

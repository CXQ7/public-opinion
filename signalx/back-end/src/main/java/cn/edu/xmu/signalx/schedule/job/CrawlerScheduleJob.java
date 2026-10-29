package cn.edu.xmu.signalx.schedule.job;

import cn.edu.xmu.signalx.record.service.NewsService;
import cn.edu.xmu.signalx.task.domain.model.OpenclawTask;
import cn.edu.xmu.signalx.task.domain.repository.OpenclawTaskRepository;
import cn.edu.xmu.signalx.task.service.OpenclawTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("!test")
public class CrawlerScheduleJob {

    private final NewsService newsService;
    private final OpenclawTaskService openclawTaskService;
    private final OpenclawTaskRepository openclawTaskRepository;
    private final JobTemplate jobTemplate;

    /**
     * Periodically start crawler.
     *
     * Default: every 5 minutes.
     */
    @Scheduled(fixedDelayString = "${schedule.crawler.fixedDelayMs:300000}")
    public void runCrawler() {
        jobTemplate.execute("CrawlerJob", newsService::executeActiveCrawlers);
    }

    /**
     * Periodically start Openclaw tasks.
     *
     * Default: every 1 minute.
     */
    @Scheduled(fixedDelayString = "${schedule.crawler.fixedDelayMsOpenclaw:60000}")
    public void runOpenclaw() {
        jobTemplate.execute("OpenclawJob", () -> {
            List<OpenclawTask> dueTasks = openclawTaskRepository.findDueTasks(LocalDateTime.now());
            if (dueTasks.isEmpty()) {
                return;
            }
            log.info("Found {} due Openclaw tasks", dueTasks.size());
            for (OpenclawTask task : dueTasks) {
                openclawTaskService.executeAndReportAsync(task);
            }
        });
    }
}

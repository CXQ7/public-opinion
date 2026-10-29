package cn.edu.xmu.signalx.schedule.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@Slf4j
public class JobTemplate {

    private final ConcurrentHashMap<String, AtomicBoolean> statusMap = new ConcurrentHashMap<>();

    public void execute(String jobName, Runnable action) {
        AtomicBoolean running = statusMap.computeIfAbsent(jobName, k -> new AtomicBoolean(false));

        if (!running.compareAndSet(false, true)) {
            log.info("Job [{}] is already running; skip this tick", jobName);
            return;
        }

        long startTime = System.currentTimeMillis();
        try {
            log.info("Starting job: [{}]", jobName);
            action.run();
            log.info("Job [{}] completed successfully in {}ms", jobName, (System.currentTimeMillis() - startTime));
        } catch (Exception e) {
            log.error("Job [{}] failed after {}ms", jobName, (System.currentTimeMillis() - startTime), e);
        } finally {
            running.set(false);
        }
    }
}

package cn.edu.xmu.signalx.schedule.job;

import cn.edu.xmu.signalx.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@Profile("!test")
public class ReportScheduleJob {

    private final ReportService reportService;
    private final JobTemplate jobTemplate;

    /**
     * Every minute: scan for due tasks and trigger report generation.
     */
    @Scheduled(fixedRateString = "${schedule.report.scanFixedRateMs:60000}")
    public void scanAndTriggerReports() {
        jobTemplate.execute("ReportScanJob", reportService::executeDueReports);
    }

    /**
     * Every 5 minutes: sync pending reports as orphan fallback.
     */
    @Scheduled(fixedRateString = "${schedule.report.fallbackSyncFixedRateMs:300000}")
    public void syncPendingReportsFallback() {
        jobTemplate.execute("ReportSyncFallbackJob", reportService::syncPendingReports);
    }
}

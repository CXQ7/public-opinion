package cn.edu.xmu.signalx.report.job;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.redis.queue.QueueMessage;
import cn.edu.xmu.signalx.core.redis.queue.RedisQueue;
import cn.edu.xmu.signalx.report.domain.model.Report;
import cn.edu.xmu.signalx.report.domain.repository.ReportRepository;
import cn.edu.xmu.signalx.report.infrastructure.client.CozeClient;
import cn.edu.xmu.signalx.report.service.ReportService;
import cn.edu.xmu.signalx.record.domain.model.News;
import cn.edu.xmu.signalx.record.service.NewsService;
import cn.edu.xmu.signalx.task.domain.model.Task;
import cn.edu.xmu.signalx.task.domain.model.TaskConfig;
import cn.edu.xmu.signalx.task.domain.model.TaskStatus;
import cn.edu.xmu.signalx.task.domain.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportSchedulerTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private CozeClient reportClient;

    @Mock
    private NewsService newsService;

    @Mock
    private RedisQueue redisQueue;

    @InjectMocks
    private ReportService reportService;

    @Test
    public void testExecuteDueReports() {
        TaskConfig config = new TaskConfig(null, null, 60);
        LocalDateTime now = LocalDateTime.now();
        // Set nextRunTime in the past and createTime in the past to pass validation
        Task t1 = Task.reconstruct(1L, 10L, Arrays.asList("A"), config, TaskStatus.ACTIVE, now.minusMinutes(70), now.minusDays(1));
        Task t2 = Task.reconstruct(2L, 10L, Arrays.asList("B"), config, TaskStatus.ACTIVE, now.minusMinutes(70), now.minusDays(1));

        when(taskRepository.findDueTasks(any(LocalDateTime.class))).thenReturn(Arrays.asList(t1, t2));

        reportService.executeDueReports();

        // Verify that tasks were pushed to the queue instead of being saved directly
        verify(redisQueue, times(2)).push(any(QueueMessage.class));
        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    public void testSyncAllPendingReports() {
        Report r1 = new Report();
        List<Report> pending = Collections.singletonList(r1);
        // Updated mock: syncReportsStatus now calls findByStatus(GENERATING)
        when(reportRepository.findByStatus(cn.edu.xmu.signalx.report.domain.model.ReportStatus.GENERATING)).thenReturn(pending);
        // Mock CozeClient return value to avoid NPE in syncSingleReportStatus
        when(reportClient.query(any())).thenReturn(CozeClient.CozeResult.running());

        reportService.syncReportsStatus();

        verify(reportRepository, times(1)).findByStatus(cn.edu.xmu.signalx.report.domain.model.ReportStatus.GENERATING);
    }
}

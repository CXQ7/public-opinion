package cn.edu.xmu.signalx.report.service;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.cos.service.CosService;
import cn.edu.xmu.signalx.report.domain.model.Report;
import cn.edu.xmu.signalx.report.domain.model.ReportStatus;
import cn.edu.xmu.signalx.report.domain.repository.ReportRepository;
import cn.edu.xmu.signalx.report.infrastructure.client.CozeClient;
import cn.edu.xmu.signalx.record.domain.model.News;
import cn.edu.xmu.signalx.record.service.NewsService;
import cn.edu.xmu.signalx.task.domain.model.Task;
import cn.edu.xmu.signalx.task.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private NewsService newsService;

    @Mock
    private CozeClient reportClient;

    @Mock
    private cn.edu.xmu.signalx.cos.service.CosService cosService;

    @Mock
    private cn.edu.xmu.signalx.user.domain.repository.UserRepository userRepository;

    @Mock
    private cn.edu.xmu.signalx.core.redis.queue.RedisQueue redisQueue;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private cn.edu.xmu.signalx.core.notify.MailNotificationService mailNotificationService;

    @InjectMocks
    private ReportService reportService;

    private Long taskId;

    @BeforeEach
    public void setup() {
        taskId = 1L;

        // Repository.save returns generated id in real impl; mock should behave similarly.
        when(reportRepository.save(any(Report.class))).thenReturn(1L);
    }

    @Test
    public void testTriggerReportWithNewsSuccess() {
        Task task = Task.create(10L, Arrays.asList("AI", "Report"), LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), 60);
        // Force nextRunTime to be in the past
        task.switchStatus(true);
        task.advanceSchedule(); // This might set it to future, let's reconstruct instead for precision
        task = Task.reconstruct(taskId, 10L, Arrays.asList("AI", "Report"), task.getConfig(), task.getStatus(), LocalDateTime.now().minusDays(1), LocalDateTime.now().minusMinutes(1));

        when(taskRepository.findById(taskId)).thenReturn(task);

        // Mock JDBC for keywords
        lenient().when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(Collections.emptyList());

        // Updated: added taskType=0
        News news = News.create(taskId, 0, "Title", "Content", "http://example.com/1", "unit-test", LocalDateTime.now().plusMinutes(1));
        PageResult<News> newsPage = new PageResult<>(List.of(news), 1L);
        when(newsService.listNews(eq(taskId), eq(0), isNull(), isNull(), eq(1), eq(50))).thenReturn(newsPage);
        when(reportClient.submitTask(anyList(), anyList())).thenReturn("run-1");

        reportService.triggerReportForTask(taskId, 0);

        verify(taskRepository, times(1)).save(any(Task.class));
        verify(reportClient, times(1)).submitTask(anyList(), anyList());

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository, atLeastOnce()).save(captor.capture());
        Report saved = captor.getValue();
        assertEquals(ReportStatus.GENERATING, saved.getStatus());
        assertEquals("run-1", saved.getCozeRunId());
    }

    @Test
    public void testTriggerReportNoNewsCompletes() {
        Task task = Task.create(10L, Arrays.asList("AI", "Report"), LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), 0);
        when(taskRepository.findById(taskId)).thenReturn(task);

        PageResult<News> newsPage = new PageResult<>(Collections.emptyList(), 0L);
        when(newsService.listNews(eq(taskId), eq(0), isNull(), isNull(), eq(1), eq(50))).thenReturn(newsPage);

        reportService.triggerReportForTask(taskId, 0);

        verify(reportClient, never()).submitTask(anyList(), anyList());

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository, atLeastOnce()).save(captor.capture());
        Report saved = captor.getValue();
        assertEquals(ReportStatus.COMPLETED, saved.getStatus());
        assertTrue(saved.getSummary().contains("未发现"));
    }

    @Test
    public void testSyncPendingReportsCompleted() {
        // Updated: added taskType=0
        Report report = Report.create(taskId, 0, "TestReport");
        report.setId(1L);
        report.bindCozeRunId("run-2");
        // Updated mock: syncReportsStatus calls findByStatus(GENERATING)
        when(reportRepository.findByStatus(ReportStatus.GENERATING)).thenReturn(List.of(report));
        when(reportClient.query("run-2")).thenReturn(CozeClient.CozeResult.completed("# Markdown"));

        reportService.syncReportsStatus();

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository, atLeastOnce()).save(captor.capture());
        Report saved = captor.getValue();
        assertEquals(ReportStatus.COMPLETED, saved.getStatus());
        assertEquals("# Markdown", saved.getSummary());
    }
}

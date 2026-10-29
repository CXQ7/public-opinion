package cn.edu.xmu.signalx.ai.strategy;

import cn.edu.xmu.signalx.ai.strategy.impl.ReportQAStrategy;
import cn.edu.xmu.signalx.report.infrastructure.mapper.ReportMapper;
import cn.edu.xmu.signalx.report.infrastructure.persistence.ReportPO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportQAStrategyTest {

    @Mock
    private ReportMapper reportMapper;

    @InjectMocks
    private ReportQAStrategy reportQAStrategy;

    @Test
    void testBuildSystemPrompt_Success() {
        Long reportId = 1L;
        ReportPO mockReport = new ReportPO();
        mockReport.setId(reportId);
        mockReport.setName("Test Report");
        mockReport.setSummary("## Summary\nContent");

        when(reportMapper.selectById(reportId)).thenReturn(mockReport);

        String prompt = reportQAStrategy.buildSystemPrompt(reportId);

        assertNotNull(prompt);
        assertTrue(prompt.contains("Test Report"));
        assertTrue(prompt.contains("## Summary"));
        assertTrue(prompt.contains("Content"));
    }

    @Test
    void testBuildSystemPrompt_NotFound() {
        Long reportId = 999L;
        when(reportMapper.selectById(reportId)).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> {
            reportQAStrategy.buildSystemPrompt(reportId);
        });
    }

    @Test
    void testGetPreferredModel() {
        assertEquals("deepseek-reasoner", reportQAStrategy.getPreferredModel());
    }
}

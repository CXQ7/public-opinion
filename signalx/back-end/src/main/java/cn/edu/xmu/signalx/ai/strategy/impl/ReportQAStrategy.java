package cn.edu.xmu.signalx.ai.strategy.impl;

import cn.edu.xmu.signalx.ai.enums.AITargetType;
import cn.edu.xmu.signalx.ai.strategy.QAContextStrategy;
import cn.edu.xmu.signalx.report.infrastructure.persistence.ReportPO;
import cn.edu.xmu.signalx.report.infrastructure.mapper.ReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReportQAStrategy implements QAContextStrategy {

    private final ReportMapper reportMapper;

    @Override
    public AITargetType getType() {
        return AITargetType.REPORT;
    }

    @Override
    public String buildSystemPrompt(Long targetId) {
        ReportPO report = reportMapper.selectById(targetId);
        if (report == null) {
            throw new IllegalArgumentException("Report not found: " + targetId);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个专业的舆情分析助手。以下是一份舆情分析报告的内容：\n\n");
        sb.append("报告标题: ").append(report.getName()).append("\n");
        sb.append("报告正文 (Markdown):\n");
        sb.append(report.getSummary()).append("\n\n"); // Assuming summary field stores the full markdown content as per schema
        sb.append("请基于这份报告的内容回答用户的问题。如果是关于报告中未提及的内容，请说明无法从报告中获取该信息。");

        return sb.toString();
    }

    @Override
    public String getPreferredModel() {
        return "deepseek-reasoner"; // Use reasoning model for deep report analysis
    }
}

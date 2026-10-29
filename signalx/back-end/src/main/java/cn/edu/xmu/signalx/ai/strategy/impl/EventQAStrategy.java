package cn.edu.xmu.signalx.ai.strategy.impl;

import cn.edu.xmu.signalx.ai.enums.AITargetType;
import cn.edu.xmu.signalx.ai.strategy.QAContextStrategy;
import cn.edu.xmu.signalx.record.infrastructure.po.NewsPO;
import cn.edu.xmu.signalx.record.infrastructure.mapper.NewsMapper;
import cn.edu.xmu.signalx.task.infrastructure.persistence.SlicePO;
import cn.edu.xmu.signalx.task.infrastructure.mapper.SliceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EventQAStrategy implements QAContextStrategy {

    private final SliceMapper sliceMapper;
    private final NewsMapper newsMapper;

    @Override
    public AITargetType getType() {
        return AITargetType.EVENT;
    }

    @Override
    public String buildSystemPrompt(Long targetId) {
        SlicePO slice = sliceMapper.selectById(targetId);
        if (slice == null) {
            throw new IllegalArgumentException("Event (Slice) not found: " + targetId);
        }

        // Fetch top related news (Limit 20)
        List<NewsPO> newsList = newsMapper.selectList(new LambdaQueryWrapper<NewsPO>()
                .eq(NewsPO::getSliceId, targetId)
                .orderByDesc(NewsPO::getPublishTime)
                .last("LIMIT 20"));

        StringBuilder sb = new StringBuilder();
        sb.append("你是一个舆情事件分析专家。以下是事件的详细资料及相关新闻报道：\n\n");
        sb.append("=== 事件基本信息 ===\n");
        sb.append("事件标题: ").append(slice.getTitle()).append("\n");
        sb.append("事件摘要: ").append(slice.getSummary()).append("\n");
        sb.append("情感倾向: ").append(slice.getSentiment()).append("\n");
        sb.append("发生时间: ").append(slice.getEventTime()).append("\n\n");

        sb.append("=== 相关新闻报道 (Top 20) ===\n");
        int index = 1;
        for (NewsPO news : newsList) {
            sb.append(index++).append(". [").append(news.getSource()).append("] ")
              .append(news.getTitle()).append(" (").append(news.getPublishTime()).append(")\n");
            // Optional: Include content snippet if needed, but summary usually suffices for context window
            if (news.getContent() != null && !news.getContent().isEmpty()) {
                String snippet = news.getContent().length() > 100 ? news.getContent().substring(0, 100) + "..." : news.getContent();
                sb.append("   摘要: ").append(snippet).append("\n");
            }
        }
        sb.append("\n请基于上述资料以及下方的历史对话信息回答用户问题。如果历史对话中已包含相关信息，请保持观点一致性。");

        return sb.toString();
    }

    @Override
    public String getPreferredModel() {
        return "deepseek-chat"; // Faster, good for context retention
    }
}

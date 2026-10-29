package cn.edu.xmu.service.impl;

import cn.edu.xmu.domain.po.Config;
import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.domain.po.Keyword;
import cn.edu.xmu.mapper.EventMapper;
import cn.edu.xmu.mapper.KeywordMapper;
import cn.edu.xmu.utils.PDFUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final KeywordMapper keywordMapper;

    private final EventMapper eventMapper;

    private Map<Keyword, List<Event>> getReportData(Config config) {
        // 获取昨天的开始时间（00:00:00）
        LocalDateTime yesterdayStart = LocalDate.now().minusDays(7).atStartOfDay();
        // 获取昨天的结束时间（23:59:59）
        LocalDateTime yesterdayEnd = yesterdayStart.plusDays(7).minusSeconds(1);
        log.info("generate report data for config id={} name={} searchKey={} dateRange={} ~ {}",
                config == null ? null : config.getId(),
                config == null ? null : config.getName(),
                config == null ? null : config.getSearchKey(),
                yesterdayStart, yesterdayEnd);
        List<Keyword> topKeywords = keywordMapper.selectList(
                new LambdaQueryWrapper<Keyword>()
                .like(Keyword::getKeyword, '%' + config.getSearchKey() + '%')
                .between(Keyword::getUpdateTime, yesterdayStart, yesterdayEnd)
                .orderByDesc(Keyword::getHeat));
        if (topKeywords == null || topKeywords.isEmpty()) {
            log.warn("no keywords found for searchKey='{}' in range {} ~ {}", config.getSearchKey(), yesterdayStart, yesterdayEnd);
            // 回退：如果按时间范围查不到，尝试不限制更新时间（以防关键词更新时间不在该范围）
            log.info("fallback: searching keywords without time range for searchKey='{}'", config.getSearchKey());
            topKeywords = keywordMapper.selectList(
                    new LambdaQueryWrapper<Keyword>()
                            .like(Keyword::getKeyword, '%' + config.getSearchKey() + '%')
                            .orderByDesc(Keyword::getHeat));
            if (topKeywords == null || topKeywords.isEmpty()) {
                log.warn("fallback also returned no keywords for searchKey='{}'", config.getSearchKey());
            } else {
                log.info("fallback found {} keywords for searchKey='{}'", topKeywords.size(), config.getSearchKey());
            }
        } else {
            log.info("found {} keywords for searchKey='{}'", topKeywords.size(), config.getSearchKey());
        }
        // 创建一个 HashMap 来存储结果
        LinkedHashMap<Keyword, List<Event>> result = new LinkedHashMap<>();

        // 遍历每个 topKeyword，获取该关键词下热度最高的前10个事件
        for (Keyword keyword : topKeywords) {
            List<Event> topEvents = eventMapper.selectTop10EventsByKeywordId(keyword.getId());
            if (topEvents == null || topEvents.isEmpty()) {
                log.info("keyword id={} keyword='{}' -> no events returned by selectTop10EventsByKeywordId", keyword.getId(), keyword.getKeyword());
            } else {
                log.info("keyword id={} keyword='{}' -> {} events", keyword.getId(), keyword.getKeyword(), topEvents.size());
            }
            eventMapper.selectList(
                    new LambdaQueryWrapper<Event>()
                    .between(Event::getUpdateTime, yesterdayStart, yesterdayEnd)
                    .orderByAsc(Event::getHeat));
            result.put(keyword, topEvents);
        }
        return result;
    }

    public String generateReportPDFByConfig(Config config) throws Exception {
        Map<Keyword, List<Event>> reportData = getReportData(config);
        return PDFUtil.generateReportPdf(config, reportData);
    }
}

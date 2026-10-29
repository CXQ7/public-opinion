package cn.edu.xmu.service.impl;

import cn.edu.xmu.domain.po.NewsRecord;
import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.domain.po.Keyword;
import cn.edu.xmu.domain.vo.KeywordVO;
import cn.edu.xmu.mapper.NewsRecordMapper;
import cn.edu.xmu.mapper.EventMapper;
import cn.edu.xmu.mapper.KeywordMapper;
import cn.edu.xmu.result.PageResult;
import cn.edu.xmu.service.AIService;
import cn.edu.xmu.service.KeywordService;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class KeywordServiceImpl extends ServiceImpl<KeywordMapper, Keyword> implements KeywordService {

    private final KeywordMapper keywordMapper;

    private final EventMapper eventMapper;
    private final NewsRecordMapper NewsRecordMapper;
    private final AIService aiService;

    @Override
    public PageResult getKeywordsList(int page, int size, String searchTerm) {
        // 开启分页，PageHelper 会自动生成分页的 SQL 语句
        PageHelper.startPage(page, size);
        List<Keyword> keywordsList = keywordMapper.getKeywordsList(searchTerm);

        // 使用 PageInfo 获取分页信息
        PageInfo<Keyword> pageInfo = new PageInfo<>(keywordsList);

        // 转换为 KeywordVO 列表，并获取最早的事件作为代表事件
        ArrayList<KeywordVO> keywordVOS = CollUtil.newArrayList(keywordsList.stream()
                .map(keyword -> {
                    // 创建并填充 KeywordVO
                    KeywordVO keywordVO = new KeywordVO();
                    BeanUtil.copyProperties(keyword, keywordVO);

                    // 获取该关键词对应的最早事件
                    Event earliestEvent = eventMapper.getEarliestEventByKeywordId(keyword.getId());

                    if (earliestEvent != null) {
                        // 设置代表事件到 KeywordVO
                        keywordVO.setRepresentativeEvent(earliestEvent);
                    }

                    return keywordVO;
                })
                .collect(Collectors.toList()));

        // 构造 PageResult 对象，返回总记录数和当前页数据
        return new PageResult(pageInfo.getTotal(), keywordVOS);
    }

    @Override
    public List<Event> getEventsByKeywordId(Long keywordId) {
        // 使用 EventsMapper 获取事件列表
        return eventMapper.getEventsByKeywordId(keywordId);
    }

    @Override
    public String analyze(Keyword keyword) {
        if(!(keyword.getAnalysis() == null) && !keyword.getAnalysis().isEmpty())
            return keyword.getAnalysis();
        String timeChainStr = getTimeChainStr(keyword);
        String queryContent = new StringBuffer()
                .append("根据以下时间轴,给出200字的概览或评价\\n")
                .append(timeChainStr)
                .toString();
        System.out.println("queryContent = "+queryContent);
        String responseStr = aiService.ask(queryContent);
        keyword.setAnalysis(responseStr);
        keywordMapper.updateById(keyword);
        return responseStr;
    }

    @Override
    public String getTimeChainStr(Keyword keyword) {
        // 创建LambdaQueryWrapper
        LambdaQueryWrapper<Event> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Event::getKeywordId, keyword.getId()); // 假设NewsRecord有一个getKeywordId方法

        // 执行查询
        List<String> queryList = new ArrayList<>();
        List<Event> events = eventMapper.selectList(queryWrapper);
        for (Event event : events) {
            List<NewsRecord> NewsRecords = NewsRecordMapper.selectBatchIds
                    (List.of(event.getNewsIds().split(" ")));

            List<String> titleList = new ArrayList<>();
            for (NewsRecord NewsRecord : NewsRecords) {
                titleList.add(NewsRecord.getTitle());
            }
            // 构建时间点描述字符串
            String timestampDesc = new StringBuffer()
                    .append(event.getEventDatetime().toString())
                    .append(": ")
                    .append(String.join(" & ", titleList))
                    .toString();
            queryList.add(timestampDesc);
        }
        // 返回时间链描述字符串
        return String.join("\\n|", queryList);
    }

    @Override
    public String askAboutAnalysis(Keyword keyword, String question) {
        String queryContent = new StringBuffer()
                .append("已知事件链:\\n")
                .append(keyword.getAnalysis())
                .append("\\n尽量简明扼要地回答:\\n")
                .append(question)
                .toString();
        System.out.println("queryContent = "+queryContent);
        return aiService.ask(queryContent);
    }
}

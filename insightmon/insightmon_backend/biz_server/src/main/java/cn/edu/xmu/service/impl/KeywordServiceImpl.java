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
import cn.edu.xmu.utils.ESUtil;
import cn.edu.xmu.utils.RedisUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
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
    private final RedisUtil redisUtil;
    private final ESUtil esUtil;

    @Override
    public PageResult getKeywordsList(int page, int pageSize, String searchTerm) {
        // 构建 Redis 缓存键
        String cacheKey = "keywordsPage:" + page + "_" + pageSize + "_" + (searchTerm!= null? "_" + searchTerm : "");
        PageResult cachedResult = (PageResult) redisUtil.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        // 1.创建Request
        SearchRequest request = new SearchRequest("keyword_vo");
        // 2.组织请求参数
        if(searchTerm == null || searchTerm.isEmpty()){
            request.source()
                    .query(QueryBuilders.matchAllQuery())
                    .sort("heat", SortOrder.DESC) // 添加按照 heat 值降序排序
                    .from((page - 1) * pageSize)
                    .size(pageSize);
        }
        else{
            request.source()
                    .query(QueryBuilders.multiMatchQuery(searchTerm, "keyword", "representativeEvent.description"))
                    .sort("heat", SortOrder.DESC) // 添加按照 heat 值降序排序
                    .from((page - 1) * pageSize)
                    .size(pageSize);
            // 2.2.高亮条件
            request.source().highlighter(
                    SearchSourceBuilder.highlight()
                            .field("keyword")
                            .field("representativeEvent.description")
                            .preTags("<em>")
                            .postTags("</em>")
            );
        }
        // 3.发送请求
        SearchResponse response = esUtil.search(request, RequestOptions.DEFAULT);
        // 4.解析响应
        PageResult pageResult = esUtil.parseResponse(response, KeywordVO.class);
        // 开启分页，PageHelper 会自动生成分页的 SQL 语句
//        PageHelper.startPage(page, size);
//        List<Keyword> keywordsList = keywordMapper.getKeywordsList(searchTerm);
//
//        // 使用 PageInfo 获取分页信息
//        PageInfo<Keyword> pageInfo = new PageInfo<>(keywordsList);
//
//        // 转换为 KeywordVO 列表，并获取最早的事件作为代表事件
//        ArrayList<KeywordVO> keywordVOS = CollUtil.newArrayList(keywordsList.stream()
//                .map(keyword -> {
//                    // 创建并填充 KeywordVO
//                    KeywordVO keywordVO = new KeywordVO();
//                    BeanUtil.copyProperties(keyword, keywordVO);
//
//                    // 获取该关键词对应的最新事件
//                    Event latestEvent = eventMapper.getLatestEventByKeywordId(keyword.getId());
//
//                    if (latestEvent != null) {
//                        // 设置代表事件到 KeywordVO
//                        keywordVO.setRepresentativeEvent(latestEvent);
//                    }
//
//                    return keywordVO;
//                })
//                .collect(Collectors.toList()));
//
//        PageResult pageResult = new PageResult(pageInfo.getTotal(), keywordVOS);
        // 将查询结果存入 Redis 缓存，并设置过期时间
        redisUtil.set(cacheKey, pageResult);
        // 构造 PageResult 对象，返回总记录数和当前页数据
        return pageResult;
    }

    @Override
    public PageResult getKeywordsListNoElastic(int page, int pageSize, String searchTerm) {
        String cacheKey = "keywordsPage:" + page + "_" + pageSize + "_" + (searchTerm!= null? "_" + searchTerm : "");
        PageResult cachedResult = (PageResult) redisUtil.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }

        // 开启分页，PageHelper 会自动生成分页的 SQL 语句
        PageHelper.startPage(page, pageSize);
        List<Keyword> keywordsList = keywordMapper.getKeywordsList(searchTerm);

        // 使用 PageInfo 获取分页信息
        PageInfo<Keyword> pageInfo = new PageInfo<>(keywordsList);

        // 转换为 KeywordVO 列表，并获取最早的事件作为代表事件
        ArrayList<KeywordVO> keywordVOS = CollUtil.newArrayList(keywordsList.stream()
                .map(keyword -> {
                    // 创建并填充 KeywordVO
                    KeywordVO keywordVO = new KeywordVO();
                    BeanUtil.copyProperties(keyword, keywordVO);

                    // 获取该关键词对应的最新事件
                    Event latestEvent = eventMapper.getLatestEventByKeywordId(keyword.getId());

                    if (latestEvent != null) {
                        // 设置代表事件到 KeywordVO
                        keywordVO.setRepresentativeEvent(latestEvent);
                    }

                    return keywordVO;
                })
                .collect(Collectors.toList()));

        // 构造 PageResult 对象，返回总记录数和当前页数据
        PageResult pageResult = new PageResult(pageInfo.getTotal(), keywordVOS);

        // 将查询结果存入 Redis 缓存，并设置过期时间
        redisUtil.set(cacheKey, pageResult);

        return pageResult;
    }

    @Override
    public List<Event> getEventsByKeywordId(Long keywordId) {
        String cacheKey = "eventsByKeywordId:" + keywordId;
        // 先从 Redis 中尝试获取数据
        List<Event> events = (List<Event>) redisUtil.get(cacheKey);
        if (events!= null) {
            return events;
        }
        List<Event> eventList = eventMapper.getEventsByKeywordId(keywordId);
        redisUtil.set(cacheKey, eventList);
        return eventList;
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

            // 理论上不会出现一个event下没有新闻的情况, 如果出现, 返回异常
            if (NewsRecords.size() == 0) {
                throw new RuntimeException("No news records found for event: " + event);
            }

            List<String> titleList = NewsRecords.stream()
                    .map(NewsRecord::getTitle)
                    .collect(Collectors.toList());

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

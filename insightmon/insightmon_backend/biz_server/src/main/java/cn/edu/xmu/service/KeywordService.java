package cn.edu.xmu.service;

import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.domain.po.Keyword;
import cn.edu.xmu.result.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface KeywordService extends IService<Keyword>{
    PageResult getKeywordsList(int page, int size, String searchTerm);

    PageResult getKeywordsListNoElastic(int page, int pageSize, String searchTerm);

    List<Event> getEventsByKeywordId(Long id);

    String analyze(Keyword keyword);

    /**
     * 根据关键词，获取时间链
     * @param keyword
     * @return
     */
    String getTimeChainStr(Keyword keyword);

    String askAboutAnalysis(Keyword keyword, String question);
}

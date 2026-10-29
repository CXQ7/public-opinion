package cn.edu.xmu.controller;

import cn.edu.xmu.domain.dto.AnalysisAskDTO;
import cn.edu.xmu.domain.po.NewsRecord;
import cn.edu.xmu.domain.po.Event;
import cn.edu.xmu.domain.po.Keyword;
import cn.edu.xmu.result.PageResult;
import cn.edu.xmu.result.Result;
import cn.edu.xmu.service.KeywordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "关键词相关接口")
@RestController
@RequestMapping("/user/keyword")
@RequiredArgsConstructor
@CrossOrigin
@Slf4j
public class KeywordController {

    private final KeywordService keywordService;

    @Operation(summary = "接入AI分析事件链")
    @GetMapping("/analyze/{id}")
    public Result<String> analyze(@PathVariable Long id){
        log.info("analyze: {}", id);
        Keyword keyword = keywordService.getById(id);
        if(keyword == null)
            return Result.error("Keyword not found");
        String res = keywordService.analyze(keyword);
        log.info("分析结果: {}", res);
        return Result.success(res);
    }

    @Operation(summary = "用户对事件链向AI提问")
    @PostMapping("/analyze/ask")
    public Result<String> analyze(@RequestBody AnalysisAskDTO askDTO){
        log.info("ask analysis: {}", askDTO);
        Keyword keyword = keywordService.getById(askDTO.getKeywordId());
        if(keyword == null)
            return Result.error("Keyword not found");
        String res = keywordService.askAboutAnalysis(keyword, askDTO.getQuestion());
        log.info("回答: {}", res);
        return Result.success(res);
    }

    @Operation(summary = "根据id查询关键词")
    @GetMapping("/{id}")
    public Keyword getKeywordById(@PathVariable("id") Long id) {
        log.info("根据id查询keyword: {}", id);
        return keywordService.getById(id);
    }

    @Operation(summary = "分页查询关键词")
    @GetMapping("/list")
    public Result<PageResult> getKeywordsList(
            @RequestParam(defaultValue = "1") int page,  // 页码，从1开始
            @RequestParam(defaultValue = "10") int pageSize,  // 每页条数
            @RequestParam(required = false) String searchTerm
    ) {
        try {
            // 获取分页数据
            PageResult pageResult = keywordService.getKeywordsListNoElastic(page, pageSize, searchTerm);
            return Result.success(pageResult);
        } catch (Exception e) {
            return Result.error("Failed to fetch keywords list: " + e.getMessage());
        }
    }

    @Operation(summary = "根据关键词id查询关联事件")
    @GetMapping("/{id}/events")
    public Result<List<Event>> getEventsByKeywordId(@PathVariable Long id) {
        try {
            List<Event> events = keywordService.getEventsByKeywordId(id);
            return Result.success(events);
        } catch (Exception e) {
            return Result.error("Failed to fetch events for keyword ID: " + id);
        }
    }
}

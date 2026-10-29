package cn.edu.xmu.signalx.record.controller;

import cn.edu.xmu.signalx.core.common.PageResult;
import cn.edu.xmu.signalx.core.common.Result;
import cn.edu.xmu.signalx.record.controller.vo.NewsVO;
import cn.edu.xmu.signalx.record.domain.model.News;
import cn.edu.xmu.signalx.record.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class RecordController {

    private final NewsService newsService;

    @GetMapping("/list")
    public Result<PageResult<NewsVO>> list(
            @RequestParam(required = false) Long taskId,
            @RequestParam(required = false, defaultValue = "0") Integer taskType,
            @RequestParam(required = false) Long sliceId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResult<News> domainResult = newsService.listNews(taskId, taskType, sliceId, keyword, page, size);

        // Convert Domain(News) to View(NewsVO)
        List<NewsVO> voList = domainResult.getList().stream().map(news -> {
            NewsVO vo = new NewsVO();
            BeanUtils.copyProperties(news, vo);
            return vo;
        }).collect(Collectors.toList());

        PageResult<NewsVO> voResult = new PageResult<>(voList, domainResult.getTotal());
        return Result.success(voResult);
    }
}

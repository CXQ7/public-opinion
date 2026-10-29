package cn.edu.xmu.controller;

import cn.edu.xmu.result.Result;
import cn.edu.xmu.service.NewsRecordService;
import cn.edu.xmu.result.PageResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "新闻相关接口")
@RestController
@RequestMapping("/user/news")
@RequiredArgsConstructor
@CrossOrigin
public class NewsRecordController {

    private final NewsRecordService newsRecordService;

    @GetMapping("/list")
    public Result<PageResult> getNewsRecordsByIds(
            @RequestParam(defaultValue = "1") int page,  // 页码，从1开始
            @RequestParam(defaultValue = "10") int pageSize,  // 每页条数
            @RequestParam String ids,
            @RequestParam(required = false) String searchTerm,  // 模糊匹配标题关键词
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startDatetime,  // 发布起始时间
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endDatetime     // 发布结束时间
    ) {
        List<Long> idList = Arrays.stream(ids.split(" "))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        PageResult pageResult = newsRecordService.getNewsRecordsByIds(page, pageSize, idList, searchTerm, startDatetime, endDatetime);
        return Result.success(pageResult);
    }

}

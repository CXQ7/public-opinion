package cn.edu.xmu.signalx.ai.controller;

import cn.edu.xmu.signalx.ai.domain.dto.AIChatResponse;
import cn.edu.xmu.signalx.ai.domain.request.AskEventRequest;
import cn.edu.xmu.signalx.ai.domain.request.AskReportRequest;
import cn.edu.xmu.signalx.ai.service.AIService;
import cn.edu.xmu.signalx.core.common.Result;
import cn.edu.xmu.signalx.core.common.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/qa")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;

    @PostMapping("/ask/report")
    public Result<AIChatResponse> askReport(@RequestBody AskReportRequest request) {
        Long userId = UserContext.getUserId();
        AIChatResponse response = aiService.askReport(userId, request.getReportId(), request.getQuestion());
        return Result.success(response);
    }

    @PostMapping("/ask/event")
    public Result<AIChatResponse> askEvent(@RequestBody AskEventRequest request) {
        Long userId = UserContext.getUserId();
        AIChatResponse response = aiService.askEvent(userId, request.getEventId(), request.getQuestion());
        return Result.success(response);
    }
}

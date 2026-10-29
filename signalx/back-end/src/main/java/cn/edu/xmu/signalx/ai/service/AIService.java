package cn.edu.xmu.signalx.ai.service;

import cn.edu.xmu.signalx.ai.domain.dto.AIChatResponse;

public interface AIService {
    AIChatResponse askReport(Long userId, Long reportId, String question);
    AIChatResponse askEvent(Long userId, Long eventId, String question);
}

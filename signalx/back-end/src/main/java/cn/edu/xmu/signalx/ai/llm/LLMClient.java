package cn.edu.xmu.signalx.ai.llm;

import cn.edu.xmu.signalx.ai.domain.dto.AIChatResponse;
import cn.edu.xmu.signalx.ai.domain.dto.DeepSeekRequest;

public interface LLMClient {
    AIChatResponse chat(DeepSeekRequest request);
}

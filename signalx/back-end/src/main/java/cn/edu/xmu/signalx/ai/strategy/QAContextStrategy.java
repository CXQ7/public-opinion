package cn.edu.xmu.signalx.ai.strategy;

import cn.edu.xmu.signalx.ai.enums.AITargetType;

public interface QAContextStrategy {
    AITargetType getType();

    /**
     * Build the system prompt/context based on the target entity data.
     * @param targetId Report ID or Event ID
     * @return System prompt string
     */
    String buildSystemPrompt(Long targetId);

    /**
     * Get preferred model for this strategy.
     * @return "deepseek-chat" or "deepseek-reasoner"
     */
    default String getPreferredModel() {
        return "deepseek-chat";
    }
}

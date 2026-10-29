package cn.edu.xmu.signalx.ai.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

import java.time.LocalDateTime;

@Data
@Builder
@TableName("qa_log")
public class QALog {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long targetId;

    /**
     * 0: REPORT, 1: EVENT
     * @see cn.edu.xmu.signalx.ai.enums.AITargetType
     */
    private Integer targetType;

    private String model;

    private String question;

    private String reasoningContent;

    private String answer;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Tolerate
    public QALog() {}
}

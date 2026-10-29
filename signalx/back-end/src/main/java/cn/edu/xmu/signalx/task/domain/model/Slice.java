package cn.edu.xmu.signalx.task.domain.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class Slice {
    @Setter
    private Long id;
    private Long taskId;
    private String title;
    private Double heat;
    private Sentiment sentiment;
    private String summary;
    private LocalDateTime eventTime;

    public static Slice create(Long taskId, String title, Double heat, Sentiment sentiment, String summary, LocalDateTime eventTime) {
        Slice slice = new Slice();
        slice.taskId = taskId;
        slice.title = title;
        slice.heat = heat != null ? heat : 0.0;
        slice.sentiment = sentiment != null ? sentiment : Sentiment.NEUTRAL;
        slice.summary = summary;
        slice.eventTime = eventTime != null ? eventTime : LocalDateTime.now();
        return slice;
    }

    public static Slice reconstruct(Long id, Long taskId, String title, Double heat, Sentiment sentiment, String summary, LocalDateTime eventTime) {
        Slice slice = new Slice();
        slice.id = id;
        slice.taskId = taskId;
        slice.title = title;
        slice.heat = heat;
        slice.sentiment = sentiment;
        slice.summary = summary;
        slice.eventTime = eventTime;
        return slice;
    }

    // 关键事件判定逻辑：只要热度高即可，不强制负面
    public boolean isCriticalEvent() {
        return heat != null && heat > 1000.0;
    }
}

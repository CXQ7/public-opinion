package cn.edu.xmu.signalx.record.controller.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NewsVO {
    private Long id;
    private Long taskId;
    private Long sliceId;
    private String title;
    private String content;
    private String url;
    private String source;
    private LocalDateTime publishTime;
    private String sentiment;
}

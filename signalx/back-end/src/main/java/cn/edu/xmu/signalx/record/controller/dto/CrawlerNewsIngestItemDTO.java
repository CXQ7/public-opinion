package cn.edu.xmu.signalx.record.controller.dto;

import lombok.Data;

@Data
public class CrawlerNewsIngestItemDTO {
    private Long taskId;
    private String title;
    private String content;
    private String url;
    private String source;

    /**
     * Publish time string.
     * Supported formats:
     * - 2026-01-29T12:34:56
     * - 2026-01-29 12:34:56
     */
    private String publishTime;
}

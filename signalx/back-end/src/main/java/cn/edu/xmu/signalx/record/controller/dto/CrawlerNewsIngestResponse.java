package cn.edu.xmu.signalx.record.controller.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CrawlerNewsIngestResponse {
    private int received;
    private int inserted;
    private int skipped;
}

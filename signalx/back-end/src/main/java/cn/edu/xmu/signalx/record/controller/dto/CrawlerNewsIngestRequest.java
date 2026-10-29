package cn.edu.xmu.signalx.record.controller.dto;

import lombok.Data;

import java.util.List;

@Data
public class CrawlerNewsIngestRequest {
    private List<CrawlerNewsIngestItemDTO> items;
}

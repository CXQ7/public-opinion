package cn.edu.xmu.signalx.ingest.controller;

import cn.edu.xmu.signalx.core.common.Result;
import cn.edu.xmu.signalx.core.config.CrawlerIngestProperties;
import cn.edu.xmu.signalx.record.controller.dto.CrawlerNewsIngestItemDTO;
import cn.edu.xmu.signalx.record.controller.dto.CrawlerNewsIngestRequest;
import cn.edu.xmu.signalx.record.controller.dto.CrawlerNewsIngestResponse;
import cn.edu.xmu.signalx.ingest.service.CrawlerIngestService;
import cn.edu.xmu.signalx.ingest.domain.model.NewsIngestItem;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/internal/crawler")
@RequiredArgsConstructor
public class CrawlerIngestController {

    private final CrawlerIngestProperties properties;
    private final CrawlerIngestService ingestService;

    @PostMapping("/news/batchUpsert")
    public Result<CrawlerNewsIngestResponse> batchUpsert(
            @RequestHeader(value = "X-Crawler-Token", required = false) String token,
            @RequestBody CrawlerNewsIngestRequest request) {
        if (!properties.isEnabled()) {
            return Result.fail(403, "Crawler ingest is disabled");
        }
        if (properties.getToken() != null && !properties.getToken().isBlank()) {
            if (token == null || !properties.getToken().equals(token)) {
                return Result.fail(401, "Invalid crawler token");
            }
        }

        List<CrawlerNewsIngestItemDTO> items = request == null ? null : request.getItems();
        List<NewsIngestItem> mapped = items == null ? List.of()
                : items.stream()
                        .map(i -> new NewsIngestItem(
                                i.getTaskId(),
                                i.getTitle(),
                                i.getContent(),
                                i.getUrl(),
                                i.getSource(),
                                i.getPublishTime()))
                        .toList();

        var result = ingestService.batchUpsert(mapped);
        return Result.success(new CrawlerNewsIngestResponse(result.received(), result.inserted(), result.skipped()));
    }
}

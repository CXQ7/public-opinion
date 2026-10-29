package cn.edu.xmu.signalx.ingest.domain.model;

public record NewsIngestItem(Long taskId, String title, String content, String url, String source, String publishTime) {
}

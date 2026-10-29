package cn.edu.xmu.signalx.ingest.domain.model;

public record IngestResult(int received, int inserted, int skipped) {
}

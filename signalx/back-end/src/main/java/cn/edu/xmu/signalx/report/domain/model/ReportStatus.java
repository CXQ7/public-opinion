package cn.edu.xmu.signalx.report.domain.model;

public enum ReportStatus {
    GENERATING(0),
    COMPLETED(1),
    FAILED(2);

    private final int code;

    ReportStatus(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }

    public static ReportStatus fromCode(Integer code) {
        if (code == null) return GENERATING;
        for (ReportStatus value : values()) {
            if (value.code == code) return value;
        }
        return GENERATING;
    }
}

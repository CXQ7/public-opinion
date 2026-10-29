package cn.edu.xmu.signalx.task.domain.model;

import lombok.Getter;

@Getter
public enum TaskStatus {
    INACTIVE(0, "停用"),
    ACTIVE(1, "启用");

    private final int code;
    private final String description;

    TaskStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static TaskStatus fromCode(Integer code) {
        if (code == null) return null;
        for (TaskStatus status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown TaskStatus code: " + code);
    }
}

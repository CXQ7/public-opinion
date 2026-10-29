package cn.edu.xmu.signalx.core.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskType {
    MONITORING(0, "Monitoring Task"),
    OPENCLAW(1, "Openclaw Task");

    private final int code;
    private final String description;

    public static TaskType fromCode(int code) {
        for (TaskType type : TaskType.values()) {
            if (type.code == code) {
                return type;
            }
        }
        return MONITORING;
    }
}

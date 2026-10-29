package cn.edu.xmu.signalx.ai.enums;

import lombok.Getter;

@Getter
public enum AITargetType {
    REPORT(0, "报告问答"),
    EVENT(1, "事件问答");

    private final int code;
    private final String desc;

    AITargetType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public static AITargetType fromCode(int code) {
        for (AITargetType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown target type code: " + code);
    }
}

package cn.edu.xmu.signalx.user.domain.model;

import lombok.Getter;

@Getter
public enum UserStatus {
    DISABLED(0),
    NORMAL(1),
    DELETED(2); // Domain logical deletion state

    private final int code;

    UserStatus(int code) {
        this.code = code;
    }

    public static UserStatus fromCode(Integer code) {
        if (code == null) return NORMAL;
        for (UserStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return NORMAL;
    }
}

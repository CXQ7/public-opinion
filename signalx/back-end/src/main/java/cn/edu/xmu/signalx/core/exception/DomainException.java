package cn.edu.xmu.signalx.core.exception;

import lombok.Getter;

@Getter
public class DomainException extends RuntimeException {

    private final int code;

    public DomainException(String message) {
        super(message);
        this.code = 4001; // Default business error code
    }

    public DomainException(int code, String message) {
        super(message);
        this.code = code;
    }
}

package cn.edu.xmu.signalx.core.exception;

import cn.edu.xmu.signalx.core.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public Result<Void> handleDomainException(DomainException e) {
        log.warn("Business Exception: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("System Error: ", e);
        return Result.fail(5000, "Internal Server Error: " + e.getMessage());
    }
}

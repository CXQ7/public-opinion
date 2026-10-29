package cn.edu.xmu.signalx.core.common;

/**
 * Global Error Codes
 * 0: Success
 * 4001-4999: Business Logic Errors
 * 5000: System Internal Errors
 */
public interface ErrorCode {

    // User Module (4001 - 4010)
    int USER_NOT_FOUND = 4001;
    int USER_ALREADY_EXISTS = 4002;
    int USER_PASSWORD_ERROR = 4003;
    int USER_ACCOUNT_LOCKED = 4004;
    int USER_UNAUTHORIZED = 4005;

    // Validation Errors
    int PARAM_VALIDATION_ERROR = 4000;
}

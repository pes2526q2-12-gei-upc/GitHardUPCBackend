package com.safesteps.backend.domain.common.exception;

public class UserForbiddenException extends RuntimeException {

    private final String errorCode;

    public UserForbiddenException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

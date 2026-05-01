package com.safesteps.backend.domain.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class UserSuspendedException extends RuntimeException {
    public UserSuspendedException(String message) {
        super(message);
    }
}
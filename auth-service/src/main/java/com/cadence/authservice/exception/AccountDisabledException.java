package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class AccountDisabledException extends AuthServiceException {
    public AccountDisabledException(String message) {
        super(ErrorCode.ACCOUNT_DISABLED, message, HttpStatus.FORBIDDEN);
    }
}

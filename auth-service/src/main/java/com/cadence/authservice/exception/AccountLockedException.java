package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class AccountLockedException extends AuthServiceException {
    public AccountLockedException(String message) {
        super(ErrorCode.ACCOUNT_LOCKED, message, HttpStatus.LOCKED);
    }
}

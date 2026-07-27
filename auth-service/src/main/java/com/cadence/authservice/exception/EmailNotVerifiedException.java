package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class EmailNotVerifiedException extends AuthServiceException {
    public EmailNotVerifiedException() {
        super(ErrorCode.EMAIL_NOT_VERIFIED, "Please verify your email address before logging in", HttpStatus.FORBIDDEN);
    }
}

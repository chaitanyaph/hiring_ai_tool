package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidMfaCodeException extends AuthServiceException {
    public InvalidMfaCodeException() {
        super(ErrorCode.INVALID_MFA_CODE, "Invalid or expired MFA code", HttpStatus.UNAUTHORIZED);
    }
}

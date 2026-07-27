package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class MfaAlreadyEnabledException extends AuthServiceException {
    public MfaAlreadyEnabledException() {
        super(ErrorCode.MFA_ALREADY_ENABLED, "Multi-factor authentication is already enabled for this account", HttpStatus.CONFLICT);
    }
}

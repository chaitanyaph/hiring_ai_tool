package com.cadence.candidateservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends CandidateServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}

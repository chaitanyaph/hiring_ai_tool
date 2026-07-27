package com.cadence.offermanagementservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends OfferManagementServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}

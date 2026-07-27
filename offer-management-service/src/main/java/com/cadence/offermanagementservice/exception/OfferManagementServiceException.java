package com.cadence.offermanagementservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class OfferManagementServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public OfferManagementServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}

package com.cadence.companyservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CompanyServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public CompanyServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}

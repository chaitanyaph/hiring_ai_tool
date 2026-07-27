package com.cadence.resumeparserservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResumeParserServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public ResumeParserServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}

package com.cadence.aiinterviewservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AiInterviewServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public AiInterviewServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}

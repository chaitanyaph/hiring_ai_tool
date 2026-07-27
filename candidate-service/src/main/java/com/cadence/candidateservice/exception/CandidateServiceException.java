package com.cadence.candidateservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class CandidateServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public CandidateServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}

package com.cadence.analyticsservice.exception;

import org.springframework.http.HttpStatus;

public class ReportGenerationException extends AnalyticsServiceException {
    public ReportGenerationException(String message, Throwable cause) {
        super(ErrorCode.EXPORT_GENERATION_FAILED, message, HttpStatus.INTERNAL_SERVER_ERROR);
        initCause(cause);
    }
}

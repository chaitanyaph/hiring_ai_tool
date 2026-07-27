package com.cadence.resumeparserservice.exception;

/**
 * Signals a failure at any pipeline checkpoint (PDF extraction, AI
 * provider call, structured-data validation). This never reaches a
 * controller/GlobalExceptionHandler -- it's caught by
 * ResumeParsingServiceImpl's orchestration method, which writes
 * status=FAILED, records a parser_log ERROR row, and publishes
 * ResumeParsingFailedEvent. Deliberately one type for all three
 * checkpoints since the handling is identical; three near-identical
 * subclasses would add ceremony without adding information.
 */
public class ResumeParsingPipelineException extends RuntimeException {
    public ResumeParsingPipelineException(String message) {
        super(message);
    }

    public ResumeParsingPipelineException(String message, Throwable cause) {
        super(message, cause);
    }
}

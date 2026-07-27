package com.cadence.aiinterviewservice.exception;

/**
 * Signals a failure at any evaluation-pipeline checkpoint (Feign call,
 * AI provider call, structured-data validation). This never reaches a
 * controller/GlobalExceptionHandler -- it's caught by the orchestration
 * service, which writes status=FAILED, records an interview_log ERROR
 * row, and publishes InterviewEvaluated with a failure reason. Same
 * one-type-for-every-checkpoint precedent as Resume Parser Service's
 * ResumeParsingPipelineException.
 */
public class InterviewPipelineException extends RuntimeException {
    public InterviewPipelineException(String message) {
        super(message);
    }

    public InterviewPipelineException(String message, Throwable cause) {
        super(message, cause);
    }
}

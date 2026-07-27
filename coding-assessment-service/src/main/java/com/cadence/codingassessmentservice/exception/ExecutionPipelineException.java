package com.cadence.codingassessmentservice.exception;

/**
 * Signals a failure at any code-execution or AI-review pipeline
 * checkpoint (Judge0 call, provider call, response parsing). Never
 * reaches a controller/GlobalExceptionHandler directly for the async
 * evaluation path -- caught by the orchestrating service, which
 * writes a FAILED-equivalent status and logs. For the synchronous
 * Run/Submit path it does surface as a 502-equivalent via
 * GlobalExceptionHandler's generic Exception handler.
 */
public class ExecutionPipelineException extends RuntimeException {
    public ExecutionPipelineException(String message) {
        super(message);
    }

    public ExecutionPipelineException(String message, Throwable cause) {
        super(message, cause);
    }
}

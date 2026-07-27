package com.cadence.resumeservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResumeServiceException.class)
    public ResponseEntity<ApiError> handleResumeServiceException(ResumeServiceException ex, HttpServletRequest request) {
        log.warn("ResumeServiceException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        return buildResponse(ex.getHttpStatus(), ex.getErrorCode(), ex.getMessage(), request, null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.FILE_TOO_LARGE, "Maximum file size is 5MB", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldValidationError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ApiError.FieldValidationError.builder().field(fe.getField()).message(fe.getDefaultMessage()).build())
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_FAILED, "One or more fields are invalid", request, fieldErrors);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiError> handleSpringAccessDenied(org.springframework.security.access.AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "You do not have permission to perform this action", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request, null);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, ErrorCode code, String message,
                                                    HttpServletRequest request, List<ApiError.FieldValidationError> fieldErrors) {
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(code)
                .message(message)
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.status(status).body(error);
    }
}

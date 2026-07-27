package com.cadence.authservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.List;

/**
 * Single, centralized place that converts every exception into a
 * consistent ApiError JSON shape. Keeping this separate from controllers
 * means controllers stay free of try/catch noise (Clean Architecture:
 * cross-cutting concerns isolated from business flow).
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthServiceException.class)
    public ResponseEntity<ApiError> handleAuthServiceException(AuthServiceException ex, HttpServletRequest request) {
        log.warn("AuthServiceException [{}]: {}", ex.getErrorCode(), ex.getMessage());
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(ex.getHttpStatus().value())
                .error(ex.getHttpStatus().getReasonPhrase())
                .errorCode(ex.getErrorCode())
                .message(ex.getMessage())
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(ex.getHttpStatus()).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.FieldValidationError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ApiError.FieldValidationError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .errorCode(ErrorCode.VALIDATION_FAILED)
                .message("One or more fields are invalid")
                .path(request.getRequestURI())
                .fieldErrors(fieldErrors)
                .build();
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCode.INVALID_CREDENTIALS, "Invalid email or password", request);
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ApiError> handleLocked(LockedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.LOCKED, ErrorCode.ACCOUNT_LOCKED, ex.getMessage(), request);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.ACCOUNT_DISABLED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, ErrorCode.ACCESS_DENIED, "You do not have permission to perform this action", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception on {}", request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.INTERNAL_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatus status, ErrorCode code, String message, HttpServletRequest request) {
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .errorCode(code)
                .message(message)
                .path(request.getRequestURI())
                .build();
        return ResponseEntity.status(status).body(error);
    }
}

package org.tw.token_billing.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.tw.token_billing.controller.dto.ErrorResponse;

import java.time.Clock;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String TOKEN_COUNT_CANNOT_BE_NEGATIVE = "Token count cannot be negative";
    private static final String REQUIRED_FIELD_IS_MISSING = "Required field is missing";

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(errorResponse(ex.getErrorCode(), ex.getErrorMessage(), request));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .filter(TOKEN_COUNT_CANNOT_BE_NEGATIVE::equals)
                .findFirst()
                .orElse(REQUIRED_FIELD_IS_MISSING);

        return ResponseEntity
                .badRequest()
                .body(errorResponse("VALIDATION_ERROR", message, request));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableMessage(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        return ResponseEntity
                .badRequest()
                .body(errorResponse("INVALID_REQUEST_BODY", "Invalid request body", request));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorResponse("INTERNAL_SERVER_ERROR", "Internal server error", request));
    }

    private ErrorResponse errorResponse(String errorCode, String message, HttpServletRequest request) {
        return new ErrorResponse(errorCode, message, Instant.now(clock), request.getRequestURI());
    }
}

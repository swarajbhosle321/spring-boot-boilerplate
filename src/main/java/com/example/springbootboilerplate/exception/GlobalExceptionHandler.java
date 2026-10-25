package com.example.springbootboilerplate.exception;

import com.example.springbootboilerplate.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Extends ResponseEntityExceptionHandler so Spring MVC's own errors (404 unknown path, 405, 415,
 * type mismatch, ...) keep their proper status instead of falling into the catch-all 500.
 * Every response, ours or Spring's, uses the same ErrorResponse body.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ErrorResponse.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ErrorResponse> handleRedisDown(RedisConnectionFailureException ex) {
        log.error("Redis unavailable", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ErrorResponse.of("SERVICE_UNAVAILABLE", "A dependency is unavailable, please retry later"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of("INTERNAL_ERROR", "Something went wrong"));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(
                new ErrorResponse("VALIDATION_FAILED", "Request validation failed", fieldErrors, Instant.now()));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return ResponseEntity.badRequest()
                .body(ErrorResponse.of("MALFORMED_REQUEST", "Request body is missing or invalid JSON"));
    }

    /**
     * Every other Spring MVC exception ends up here: keep Spring's status and headers (e.g. Allow on 405),
     * swap the body for our format. Code is the status name, e.g. NOT_FOUND, METHOD_NOT_ALLOWED.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String code = status != null ? status.name() : "HTTP_" + statusCode.value();
        String message = ex instanceof org.springframework.web.ErrorResponse springError
                && springError.getBody().getDetail() != null
                ? springError.getBody().getDetail()
                : (status != null ? status.getReasonPhrase() : "Request could not be processed");
        return ResponseEntity.status(statusCode).headers(headers).body(ErrorResponse.of(code, message));
    }
}

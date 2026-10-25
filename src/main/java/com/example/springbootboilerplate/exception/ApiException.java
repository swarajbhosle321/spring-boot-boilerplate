package com.example.springbootboilerplate.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base class for errors that map to a known HTTP status and error code.
 */
@Getter
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    protected ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }
}

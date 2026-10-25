package com.example.springbootboilerplate.exception;

import org.springframework.http.HttpStatus;

public class LockNotAcquiredException extends ApiException {

    public LockNotAcquiredException() {
        super(HttpStatus.CONFLICT, "REQUEST_IN_PROGRESS",
                "The same request is already being processed, please retry shortly");
    }
}

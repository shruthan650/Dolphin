package com.dolphin.exception;

import org.springframework.http.HttpStatus;

/** Login rejected because the account is temporarily locked after repeated wrong passwords. */
public class TooManyAttemptsException extends ApiException {

    public TooManyAttemptsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}

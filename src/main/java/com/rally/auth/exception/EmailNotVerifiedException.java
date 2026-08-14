package com.rally.auth.exception;

import com.rally.common.exceptions.base.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Mapped by the shared GlobalExceptionHandler to the exact ErrorResponse shape.
 * Thrown when login is attempted on an account whose email is not verified (403).
 */
public class EmailNotVerifiedException extends BaseException {

    public EmailNotVerifiedException() {
        super(HttpStatus.FORBIDDEN, "Email verification is required before logging in.", "Email.Unverified");
    }
}
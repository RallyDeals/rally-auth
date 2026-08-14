package com.rally.auth.exception;

import com.rally.common.exceptions.base.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Mapped by the shared GlobalExceptionHandler to the exact ErrorResponse shape.
 * Thrown when an email verification code is invalid, expired, already used, or
 * exhausted (401).
 */
public class InvalidOtpException extends BaseException {

    public InvalidOtpException() {
        super(HttpStatus.UNAUTHORIZED, "Invalid or expired verification code.", "Email.Otp.Invalid");
    }
}
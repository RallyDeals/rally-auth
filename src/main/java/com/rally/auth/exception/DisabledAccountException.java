package com.rally.auth.exception;

import com.rally.common.exceptions.base.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Mapped by the shared GlobalExceptionHandler to the exact ErrorResponse shape.
 * Thrown when login or renewal is attempted on a disabled account (403).
 */
public class DisabledAccountException extends BaseException {

    public DisabledAccountException() {
        super(HttpStatus.FORBIDDEN, "This account is disabled.", "Account.Disabled");
    }
}
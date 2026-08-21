package com.rally.auth.exception;

import com.rally.common.exceptions.base.BaseException;
import org.springframework.http.HttpStatus;

/**
 * Mapped by the shared GlobalExceptionHandler to the exact ErrorResponse shape.
 * Thrown when a role change is attempted by a non-admin caller or targets an
 * ADMIN account (403).
 */
public class RoleChangeNotAllowedException extends BaseException {

    public RoleChangeNotAllowedException() {
        super(HttpStatus.FORBIDDEN, "Role change is not allowed.", "Role.ChangeNotAllowed");
    }
}

package com.rally.auth.exception;

import com.rally.common.exceptions.base.BaseException;
import org.springframework.http.HttpStatus;

public class InvalidCurrentPassword extends BaseException {

    public InvalidCurrentPassword() {
        super(HttpStatus.BAD_REQUEST, "Identity.InvalidPassword", "Invalid Current Password");
    }
}

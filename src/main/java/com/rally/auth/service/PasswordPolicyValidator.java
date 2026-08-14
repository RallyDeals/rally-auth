package com.rally.auth.service;

import com.rally.auth.config.AppProperties;
import com.rally.common.exceptions.shared.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class PasswordPolicyValidator {

    private final AppProperties.Password policy;

    public PasswordPolicyValidator(AppProperties appProperties) {
        this.policy = appProperties.getPassword();
    }

    public void validate(String password) {
        if (password.length() < policy.getMinLength()) {
            throw new ValidationException(
                    "Password must be at least " + policy.getMinLength() + " characters.");
        }
        if (password.length() > policy.getMaxLength()) {
            throw new ValidationException(
                    "Password must be at most " + policy.getMaxLength() + " characters.");
        }
        if (policy.isRequireUppercase() && !password.matches(".*[A-Z].*")) {
            throw new ValidationException("Password must contain at least one uppercase letter.");
        }
        if (policy.isRequireLowercase() && !password.matches(".*[a-z].*")) {
            throw new ValidationException("Password must contain at least one lowercase letter.");
        }
        if (policy.isRequireDigit() && !password.matches(".*[0-9].*")) {
            throw new ValidationException("Password must contain at least one digit.");
        }
        if (policy.isRequireSpecial() && password.matches("[a-zA-Z0-9]*")) {
            throw new ValidationException("Password must contain at least one special character.");
        }
    }
}
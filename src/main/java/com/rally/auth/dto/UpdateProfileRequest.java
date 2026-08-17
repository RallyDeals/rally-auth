package com.rally.auth.dto;

import java.util.Optional;

public record UpdateProfileRequest(
        Optional<String> firstName,
        Optional<String> lastName,
        Optional<String> phoneNumber
) {
}
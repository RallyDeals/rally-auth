package com.rally.auth.dto;

import com.rally.auth.domain.user.Role;
import java.time.Instant;
import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}
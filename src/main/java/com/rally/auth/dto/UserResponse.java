package com.rally.auth.dto;

import com.rally.auth.domain.user.Role;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        String profilePicture,
        Role role,
        boolean enabled,
        boolean emailVerified,
        Instant emailVerifiedAt,
        Instant createdAt
) {
}
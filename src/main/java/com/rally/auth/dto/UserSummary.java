package com.rally.auth.dto;

import com.rally.auth.domain.user.Role;
import java.util.UUID;

public record UserSummary(
        UUID id,
        String firstName,
        String lastName,
        String email,
        Role role
) {
}
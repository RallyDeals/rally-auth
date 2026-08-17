package com.rally.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record UserListItem(
        UUID id,
        String name,
        String email,
        String avatarUrl,
        Instant joinedAt,
        String status,
        String type
) {
}
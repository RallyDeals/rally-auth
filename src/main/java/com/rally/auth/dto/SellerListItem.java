package com.rally.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record SellerListItem(
        UUID id,
        String name,
        String email,
        String avatarUrl,
        Instant joinedAt
) {
}
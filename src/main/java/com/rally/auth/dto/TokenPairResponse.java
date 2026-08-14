package com.rally.auth.dto;

public record TokenPairResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
package com.rally.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RegisterRequest(
        @NotBlank String firstName,
        String lastName,
        @NotBlank @Email String email,
        @NotBlank String password,
        String phoneNumber,
        @NotNull @Pattern(regexp = "BUYER|SELLER", message = "Role must be BUYER or SELLER") String role
) {
}
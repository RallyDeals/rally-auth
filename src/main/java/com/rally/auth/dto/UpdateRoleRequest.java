package com.rally.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateRoleRequest(
        @NotNull @Pattern(regexp = "BUYER|SELLER|ADMIN", message = "Role must be BUYER, SELLER or ADMIN") String role) {
}

package com.rally.auth.dto;

import com.rally.auth.domain.user.Role;
import java.util.UUID;

public record UserRoleResponse(UUID id, Role role) {
}

package com.rally.auth.api;

import com.rally.auth.dto.UpdateProfileRequest;
import com.rally.auth.dto.UserResponse;
import com.rally.auth.service.ProfileService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final ProfileService profileService;

    public UserController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId) {
        return ResponseEntity.ok(profileService.getProfileForAdmin(id, adminId));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(profileService.updateUserProfile(id, adminId, request));
    }
}

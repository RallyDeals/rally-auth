package com.rally.auth.api;

import com.rally.auth.dto.*;
import com.rally.auth.service.AdminUserService;
import com.rally.auth.service.ProfileService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/users")
public class UserController {

    private final ProfileService profileService;
    private final AdminUserService adminUserService;

    public UserController(ProfileService profileService, AdminUserService adminUserService) {
        this.profileService = profileService;
        this.adminUserService = adminUserService;
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

    @GetMapping
    public ResponseEntity<PageResponse<UserListItem>> listUsers(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) List<String> types,
            @RequestParam(required = false) List<String> statuses) {
        return ResponseEntity.ok(
                adminUserService.listUsers(adminId, page, limit, search, types, statuses));
    }

    @GetMapping("/sellers")
    public ResponseEntity<PageResponse<SellerListItem>> listSellers(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(adminUserService.listSellers(adminId, page, limit, search));
    }

    @PatchMapping("/{id}/ban")
    public ResponseEntity<Void> banUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId) {
        adminUserService.ban(adminId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<Void> activateUser(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId) {
        adminUserService.activate(adminId, id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/role")
    public ResponseEntity<UserResponse> changeRole(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId,
            @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(adminUserService.changeRole(adminId, id, request));
    }

    @GetMapping("/{id}/roles")
    public ResponseEntity<UserRoleResponse> getUserRoles(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId) {
        return ResponseEntity.ok(adminUserService.getRoles(adminId, id));
    }

    @GetMapping("/sellers/{id}")
    public ResponseEntity<SellerListItem> getSeller(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") UUID adminId) {
        return ResponseEntity.ok(adminUserService.getSeller(adminId, id));
    }

    @GetMapping("/batch")
    public ResponseEntity<List<UserSummary>> getUsersBatch(
            @RequestBody List<UUID> userIds) {
        return ResponseEntity.ok(profileService.getUsersBatch(userIds));
    }
}

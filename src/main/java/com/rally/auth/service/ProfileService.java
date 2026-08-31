package com.rally.auth.service;

import com.rally.auth.domain.user.Role;
import com.rally.auth.domain.user.User;
import com.rally.auth.dto.UpdateProfileRequest;
import com.rally.auth.dto.UserResponse;
import com.rally.auth.dto.UserSummary;
import com.rally.auth.repository.UserJpaRepository;
import com.rally.common.exceptions.shared.NotFoundException;
import com.rally.common.exceptions.shared.UnauthorizedException;
import com.rally.common.exceptions.shared.ValidationException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private final UserJpaRepository userJpaRepository;

    public ProfileService(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        User user = findUser(userId);
        log.debug("Profile viewed userId={}", userId);
        return toUserResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfileForAdmin(UUID targetId, UUID adminId) {
        requireAdmin(adminId);
        User user = findUser(targetId);
        log.debug("Profile viewed by admin adminId={} targetUserId={}", adminId, targetId);
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateOwnProfile(UUID callerId, UpdateProfileRequest request) {
        User user = applyUpdate(callerId, request);
        log.debug("Profile updated by self userId={}", callerId);
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateUserProfile(UUID targetId, UUID adminId, UpdateProfileRequest request) {
        requireAdmin(adminId);
        User user = applyUpdate(targetId, request);
        log.debug("Profile updated by admin adminId={} targetUserId={}", adminId, targetId);
        return toUserResponse(user);
    }

    @Transactional
    public List<UserSummary> getUsersBatch(List<UUID> userIds) {
        List<User> users = userJpaRepository.findByIdIn(userIds);
        return users.stream()
                .map(this::toUserSummary)
                .collect(Collectors.toList());
    }

    private User applyUpdate(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        String firstName = request.firstName().map(this::validateFirstName).orElse(null);
        String lastName = request.lastName().map(this::validateLastName).orElse(null);
        String phoneNumber = request.phoneNumber().map(this::validatePhoneNumber).orElse(null);

        user.applyProfile(firstName, lastName, phoneNumber);
        return userJpaRepository.save(user);
    }

    private User findUser(UUID userId) {
        return userJpaRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    private void requireAdmin(UUID adminId) {
        User admin = findUser(adminId);
        if (admin.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Admin access required");
        }
    }

    private String validateFirstName(String value) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException("First name must not be blank");
        }
        if (trimmed.length() > 100) {
            throw new ValidationException("First name must be at most 100 characters");
        }
        return trimmed;
    }

    private String validateLastName(String value) {
        String trimmed = value.trim();
        if (trimmed.length() > 100) {
            throw new ValidationException("Last name must be at most 100 characters");
        }
        return trimmed;
    }

    private String validatePhoneNumber(String value) {
        String trimmed = value.trim();
        if (trimmed.length() > 30) {
            throw new ValidationException("Phone number must be at most 30 characters");
        }
        return trimmed;
    }

    private UserResponse toUserResponse(User user) {
        return new UserResponse(
                user.getId(),
                fullName(user),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                null,
                user.getRole(),
                user.isEnabled(),
                user.isEmailVerified(),
                user.getEmailVerifiedAt(),
                user.getCreatedAt());
    }

    private UserSummary toUserSummary(User user){
        return new UserSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole()
        );
    }

    private String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        if (lastName.isEmpty()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
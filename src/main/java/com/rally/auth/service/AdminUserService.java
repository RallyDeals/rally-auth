package com.rally.auth.service;

import com.rally.auth.domain.user.Role;
import com.rally.auth.domain.user.User;
import com.rally.auth.dto.PageResponse;
import com.rally.auth.dto.SellerListItem;
import com.rally.auth.dto.UpdateRoleRequest;
import com.rally.auth.dto.UserListItem;
import com.rally.auth.dto.UserResponse;
import com.rally.auth.dto.UserRoleResponse;
import com.rally.auth.exception.RoleChangeNotAllowedException;
import com.rally.auth.repository.RefreshTokenJpaRepository;
import com.rally.auth.repository.UserJpaRepository;
import com.rally.common.exceptions.shared.NotFoundException;
import com.rally.common.exceptions.shared.UnauthorizedException;
import com.rally.common.exceptions.shared.ValidationException;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private static final int DEFAULT_LIMIT = 20;
    private static final int MAX_LIMIT = 100;

    private final UserJpaRepository userJpaRepository;
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;

    public AdminUserService(UserJpaRepository userJpaRepository, RefreshTokenJpaRepository refreshTokenJpaRepository) {
        this.userJpaRepository = userJpaRepository;
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserListItem> listUsers(
            UUID adminId, int page, int limit, String search, List<String> types, List<String> statuses) {
        requireAdmin(adminId);
        List<Role> roleFilters = mapTypes(types);
        List<Boolean> statusFilters = mapStatuses(statuses);
        Specification<User> spec = accountSpec(search, roleFilters, statusFilters);
        return listUsers(spec, page, limit, this::toUserListItem);
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerListItem> listSellers(UUID adminId, int page, int limit, String search) {
        requireAdmin(adminId);
        Specification<User> spec = accountSpec(search, List.of(Role.SELLER), null);
        return listUsers(spec, page, limit, this::toSellerListItem);
    }

    private <T> PageResponse<T> listUsers(
            Specification<User> spec, int page, int limit, java.util.function.Function<User, T> mapper) {
        int pageNumber = Math.max(page, 1);
        int pageSize = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, MAX_LIMIT);
        Page<User> result = userJpaRepository.findAll(
                spec,
                PageRequest.of(pageNumber - 1, pageSize,
                        Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.ASC, "id"))));
        return new PageResponse<>(
                result.getContent().stream().map(mapper).toList(),
                pageNumber,
                pageSize,
                result.getTotalElements());
    }

    private Specification<User> accountSpec(String search, List<Role> types, List<Boolean> statuses) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String term = search == null ? null : search.trim();
            if (term != null && !term.isEmpty()) {
                String pattern = "%" + term.toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)));
            }
            if (types != null && !types.isEmpty()) {
                predicates.add(root.get("role").in(types));
            }
            if (statuses != null && !statuses.isEmpty()) {
                predicates.add(root.get("enabled").in(statuses));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private List<Role> mapTypes(List<String> types) {
        if (types == null || types.isEmpty()) {
            return null;
        }
        return types.stream().map(type -> switch (type.toLowerCase()) {
            case "buyer" -> Role.BUYER;
            case "seller" -> Role.SELLER;
            case "admin" -> Role.ADMIN;
            default -> throw new ValidationException("Invalid account type: " + type);
        }).toList();
    }

    private List<Boolean> mapStatuses(List<String> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            return null;
        }
        return statuses.stream().map(status -> switch (status.toLowerCase()) {
            case "active" -> Boolean.TRUE;
            case "banned" -> Boolean.FALSE;
            default -> throw new ValidationException("Invalid account status: " + status);
        }).toList();
    }

    private UserListItem toUserListItem(User user) {
        return new UserListItem(
                user.getId(),
                fullName(user),
                user.getEmail(),
                null,
                user.getCreatedAt(),
                user.isEnabled() ? "active" : "banned",
                switch (user.getRole()) {
                    case ADMIN -> "admin";
                    case SELLER -> "seller";
                    case BUYER -> "buyer";
                });
    }

    private SellerListItem toSellerListItem(User user) {
        return new SellerListItem(
                user.getId(),
                fullName(user),
                user.getEmail(),
                null,
                user.getCreatedAt());
    }

    @Transactional
    public void ban(UUID adminId, UUID targetId) {
        requireAdmin(adminId);
        User target = findUser(targetId);
        if (target.getRole() == Role.ADMIN) {
            throw new ValidationException("Cannot ban an ADMIN account");
        }
        target.disable();
        refreshTokenJpaRepository.revokeAllByUserId(targetId);
        userJpaRepository.save(target);
    }

    @Transactional
    public void activate(UUID adminId, UUID targetId) {
        requireAdmin(adminId);
        User target = findUser(targetId);
        if (target.getRole() == Role.ADMIN) {
            throw new ValidationException("Cannot activate an ADMIN account");
        }
        target.enable();
        userJpaRepository.save(target);
    }

    @Transactional(readOnly = true)
    public SellerListItem getSeller(UUID adminId, UUID targetId) {
        requireAdmin(adminId);
        User user = findUser(targetId);
        if (user.getRole() != Role.SELLER) {
            throw new NotFoundException("User", targetId);
        }
        return toSellerListItem(user);
    }

    @Transactional(readOnly = true)
    public UserRoleResponse getRoles(UUID adminId, UUID targetId) {
        requireAdmin(adminId);
        User user = findUser(targetId);
        return new UserRoleResponse(user.getId(), user.getRole());
    }

    @Transactional
    public UserResponse changeRole(UUID adminId, UUID targetId, UpdateRoleRequest request) {
        User caller = findUser(adminId);
        if (caller.getRole() != Role.ADMIN) {
            throw new RoleChangeNotAllowedException();
        }
        User target = findUser(targetId);
        if (target.getRole() == Role.ADMIN) {
            throw new RoleChangeNotAllowedException();
        }
        Role newRole = resolveRole(request.role());
        if (target.getRole() == newRole) {
            return toUserResponse(target);
        }
        target.changeRole(newRole);
        userJpaRepository.save(target);
        refreshTokenJpaRepository.revokeAllByUserId(targetId);
        return toUserResponse(target);
    }

    private Role resolveRole(String role) {
        if (role == null) {
            throw new ValidationException("Role must be BUYER, SELLER or ADMIN");
        }
        return switch (role) {
            case "BUYER" -> Role.BUYER;
            case "SELLER" -> Role.SELLER;
            case "ADMIN" -> Role.ADMIN;
            default -> throw new ValidationException("Role must be BUYER, SELLER or ADMIN");
        };
    }

    private void requireAdmin(UUID adminId) {
        User admin = findUser(adminId);
        if (admin.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Admin access required");
        }
    }

    private User findUser(UUID userId) {
        return userJpaRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
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

    private String fullName(User user) {
        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        if (lastName.isEmpty()) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
}
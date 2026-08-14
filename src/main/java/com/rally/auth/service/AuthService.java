package com.rally.auth.service;

import com.rally.auth.config.AppProperties;
import com.rally.auth.domain.token.RefreshToken;
import com.rally.auth.domain.user.User;
import com.rally.auth.dto.ChangePasswordRequest;
import com.rally.auth.dto.LoginRequest;
import com.rally.auth.dto.LoginResponse;
import com.rally.auth.dto.UserSummary;
import com.rally.auth.exception.DisabledAccountException;
import com.rally.auth.exception.EmailNotVerifiedException;
import com.rally.auth.repository.RefreshTokenJpaRepository;
import com.rally.auth.repository.UserJpaRepository;
import com.rally.auth.security.JwtTokenService;
import com.rally.auth.security.RefreshTokenFactory;
import com.rally.common.exceptions.domain.auth.InvalidCredentialsException;
import com.rally.common.exceptions.shared.NotFoundException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserJpaRepository userJpaRepository;
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final AppProperties appProperties;

    public AuthService(
            UserJpaRepository userJpaRepository,
            RefreshTokenJpaRepository refreshTokenJpaRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            PasswordPolicyValidator passwordPolicyValidator,
            AppProperties appProperties) {
        this.userJpaRepository = userJpaRepository;
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.appProperties = appProperties;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        Optional<User> byEmail = userJpaRepository.findByEmail(normalize(request.email()));
        if (byEmail.isEmpty()) {
            log.warn("Failed login attempt for unknown email");
            throw new InvalidCredentialsException();
        }
        User user = byEmail.get();
        if (!user.isEnabled()) {
            log.warn("Failed login attempt for disabled account userId={}", user.getId());
            throw new DisabledAccountException();
        }
        if (!user.isEmailVerified() && !appProperties.isAutoConfirmEmail()) {
            log.warn("Failed login attempt for unverified account userId={}", user.getId());
            throw new EmailNotVerifiedException();
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("Failed login attempt for wrong password userId={}", user.getId());
            throw new InvalidCredentialsException();
        }

        LoginResponse response = issueTokenPair(user);
        log.debug("User logged in userId={}", user.getId());
        return response;
    }

    @Transactional
    public LoginResponse issueTokenPair(User user) {
        String refreshToken = issueRefreshToken(user.getId());
        String accessToken = jwtTokenService.sign(user, user.getRole());
        return new LoginResponse(
                accessToken,
                refreshToken,
                appProperties.getSecurity().getAccessTokenSeconds(),
                new UserSummary(
                        user.getId(), user.getFirstName(), user.getLastName(),
                        user.getEmail(), user.getRole()));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userJpaRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        passwordPolicyValidator.validate(request.newPassword());

        user.changePassword(passwordEncoder.encode(request.newPassword()));
        userJpaRepository.save(user);
        refreshTokenJpaRepository.revokeAllByUserId(userId);
        log.warn("Password changed and all sessions revoked userId={}", userId);
    }

    private String issueRefreshToken(UUID userId) {
        String value = RefreshTokenFactory.newValue();
        Instant expirationDate = Instant.now()
                .plus(appProperties.getSecurity().getRefreshTokenDays(), ChronoUnit.DAYS);
        refreshTokenJpaRepository.save(
                RefreshToken.issue(userId, RefreshTokenFactory.digest(value), expirationDate));
        return value;
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
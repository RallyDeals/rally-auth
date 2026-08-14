package com.rally.auth.service;

import com.rally.auth.config.AppProperties;
import com.rally.auth.domain.token.RefreshToken;
import com.rally.auth.domain.user.User;
import com.rally.auth.dto.LogoutRequest;
import com.rally.auth.dto.RefreshRequest;
import com.rally.auth.dto.TokenPairResponse;
import com.rally.auth.exception.DisabledAccountException;
import com.rally.auth.repository.RefreshTokenJpaRepository;
import com.rally.auth.repository.UserJpaRepository;
import com.rally.auth.security.JwtTokenService;
import com.rally.auth.security.RefreshTokenFactory;
import com.rally.common.exceptions.domain.auth.InvalidRefreshTokenException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private static final Logger log = LoggerFactory.getLogger(SessionService.class);

    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final UserJpaRepository userJpaRepository;
    private final JwtTokenService jwtTokenService;
    private final AppProperties appProperties;

    public SessionService(
            RefreshTokenJpaRepository refreshTokenJpaRepository,
            UserJpaRepository userJpaRepository,
            JwtTokenService jwtTokenService,
            AppProperties appProperties) {
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
        this.userJpaRepository = userJpaRepository;
        this.jwtTokenService = jwtTokenService;
        this.appProperties = appProperties;
    }

    @Transactional
    public TokenPairResponse refresh(RefreshRequest request) {
        RefreshToken token = refreshTokenJpaRepository
                .findByToken(RefreshTokenFactory.digest(request.refreshToken()))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!token.isValid()) {
            throw new InvalidRefreshTokenException();
        }
        User user = userJpaRepository.findById(token.getUserId())
                .orElseThrow(InvalidRefreshTokenException::new);
        if (!user.isEnabled()) {
            log.warn("Failed renewal attempt for disabled account userId={}", user.getId());
            throw new DisabledAccountException();
        }

        token.revoke();
        String refreshToken = issueRefreshToken(user.getId());
        String accessToken = jwtTokenService.sign(user, user.getRole());
        log.debug("Session renewed userId={}", user.getId());
        return new TokenPairResponse(
                accessToken,
                refreshToken,
                appProperties.getSecurity().getAccessTokenSeconds());
    }

    @Transactional
    public void logout(LogoutRequest request) {
        refreshTokenJpaRepository
                .findByToken(RefreshTokenFactory.digest(request.refreshToken()))
                .ifPresent(token -> {
                    if (!token.isRevoked()) {
                        token.revoke();
                    }
                });
        log.debug("Session ended");
    }

    private String issueRefreshToken(UUID userId) {
        String value = RefreshTokenFactory.newValue();
        Instant expirationDate = Instant.now()
                .plus(appProperties.getSecurity().getRefreshTokenDays(), ChronoUnit.DAYS);
        refreshTokenJpaRepository.save(
                RefreshToken.issue(userId, RefreshTokenFactory.digest(value), expirationDate));
        return value;
    }
}
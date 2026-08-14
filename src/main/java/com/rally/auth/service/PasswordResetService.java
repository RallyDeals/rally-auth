package com.rally.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.auth.config.AppProperties;
import com.rally.auth.domain.otp.EmailOtp;
import com.rally.auth.domain.otp.OtpPurpose;
import com.rally.auth.domain.user.User;
import com.rally.auth.exception.InvalidOtpException;
import com.rally.auth.messaging.contract.UserEventTypes;
import com.rally.auth.messaging.outbox.OutboxEventWriter;
import com.rally.auth.repository.EmailOtpJpaRepository;
import com.rally.auth.repository.RefreshTokenJpaRepository;
import com.rally.auth.repository.UserJpaRepository;
import com.rally.auth.security.OtpGenerator;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserJpaRepository userJpaRepository;
    private final EmailOtpJpaRepository emailOtpJpaRepository;
    private final RefreshTokenJpaRepository refreshTokenJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final OtpGenerator otpGenerator;
    private final OtpAttemptRecorder otpAttemptRecorder;
    private final OutboxEventWriter outboxEventWriter;
    private final AppProperties appProperties;

    public PasswordResetService(
            UserJpaRepository userJpaRepository,
            EmailOtpJpaRepository emailOtpJpaRepository,
            RefreshTokenJpaRepository refreshTokenJpaRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            OtpGenerator otpGenerator,
            OtpAttemptRecorder otpAttemptRecorder,
            OutboxEventWriter outboxEventWriter,
            AppProperties appProperties) {
        this.userJpaRepository = userJpaRepository;
        this.emailOtpJpaRepository = emailOtpJpaRepository;
        this.refreshTokenJpaRepository = refreshTokenJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.otpGenerator = otpGenerator;
        this.otpAttemptRecorder = otpAttemptRecorder;
        this.outboxEventWriter = outboxEventWriter;
        this.appProperties = appProperties;
    }

    @Transactional
    public void requestPasswordReset(String email) {
        String normalized = normalize(email);
        Optional<User> byEmail = userJpaRepository.findByEmail(normalized);
        if (byEmail.isEmpty()) {
            log.debug("Reset request for unknown email (enum-safe no-op)");
            return;
        }
        User user = byEmail.get();

        Optional<EmailOtp> latest = emailOtpJpaRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(normalized, OtpPurpose.PASSWORD_RESET);
        if (latest.isPresent() && withinCooldown(latest.get())) {
            log.debug("Reset request within cooldown (no new code issued) email={}", normalized);
            return;
        }

        issueResetCode(normalized, user.getId());
        log.warn("New reset code requested userId={}", user.getId());
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        String normalized = normalize(email);
        User user = userJpaRepository.findByEmail(normalized)
                .orElseThrow(InvalidOtpException::new);

        EmailOtp resetCode = emailOtpJpaRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(normalized, OtpPurpose.PASSWORD_RESET)
                .orElseThrow(InvalidOtpException::new);

        int maxAttempts = appProperties.getOtp().getMaxAttempts();
        if (!resetCode.isValid() || resetCode.isExhausted(maxAttempts)) {
            throw new InvalidOtpException();
        }
        if (!resetCode.getOtp().equals(otp)) {
            otpAttemptRecorder.recordFailedAttempt(resetCode.getId());
            log.warn("Failed password reset attempt userId={} attempts={} of {}",
                    user.getId(), resetCode.getFailedAttempts() + 1, maxAttempts);
            throw new InvalidOtpException();
        }

        passwordPolicyValidator.validate(newPassword);

        resetCode.markUsed();
        emailOtpJpaRepository.save(resetCode);
        user.changePassword(passwordEncoder.encode(newPassword));
        user.verifyEmail();
        userJpaRepository.save(user);
        refreshTokenJpaRepository.revokeAllByUserId(user.getId());

        log.debug("Password reset completed userId={}", user.getId());
    }

    private void issueResetCode(String email, UUID userId) {
        String code = otpGenerator.generate();
        Instant expiresAt = Instant.now()
                .plus(appProperties.getOtp().getExpirationMinutes(), ChronoUnit.MINUTES);
        emailOtpJpaRepository.revokePendingByEmailAndPurpose(email, OtpPurpose.PASSWORD_RESET);
        EmailOtp otp = EmailOtp.generate(email, OtpPurpose.PASSWORD_RESET, code, expiresAt);
        emailOtpJpaRepository.save(otp);

        UUID requestId = UUID.randomUUID();
        if (appProperties.getEmail().isEnabled()) {
            outboxEventWriter.writeUserEvent(
                    userId,
                    UserEventTypes.PASSWORD_RESET_REQUESTED,
                    OBJECT_MAPPER.valueToTree(Map.of(
                            "userId", userId.toString(),
                            "email", email,
                            "otpRequestId", otp.getId().toString())),
                    requestId);
        } else {
            log.warn("Email delivery disabled: reset code for {} is {}", email, code);
        }
    }

    private boolean withinCooldown(EmailOtp latest) {
        Duration elapsed = Duration.between(latest.getCreatedAt(), Instant.now());
        return elapsed.getSeconds() < appProperties.getOtp().getResendCooldownSeconds();
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}

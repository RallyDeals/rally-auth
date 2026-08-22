package com.rally.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rally.auth.config.AppProperties;
import com.rally.auth.domain.otp.EmailOtp;
import com.rally.auth.domain.otp.OtpPurpose;
import com.rally.auth.domain.user.Role;
import com.rally.auth.domain.user.User;
import com.rally.auth.dto.LoginResponse;
import com.rally.auth.dto.RegisterRequest;
import com.rally.auth.dto.RegisterResponse;
import com.rally.auth.dto.ResendVerificationRequest;
import com.rally.auth.dto.VerifyEmailRequest;
import com.rally.auth.exception.InvalidOtpException;
import com.rally.auth.messaging.contract.UserEventTypes;
import com.rally.auth.messaging.outbox.OutboxEventWriter;
import com.rally.auth.repository.EmailOtpJpaRepository;
import com.rally.auth.repository.UserJpaRepository;
import com.rally.auth.security.OtpEncryptor;
import com.rally.auth.security.OtpGenerator;
import com.rally.common.exceptions.domain.auth.UserAlreadyExistsException;
import com.rally.common.exceptions.shared.ValidationException;
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
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserJpaRepository userJpaRepository;
    private final EmailOtpJpaRepository emailOtpJpaRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicyValidator passwordPolicyValidator;
    private final OtpGenerator otpGenerator;
    private final OtpEncryptor otpEncryptor;
    private final OutboxEventWriter outboxEventWriter;
    private final AuthService authService;
    private final AppProperties appProperties;
    private final OtpAttemptRecorder otpAttemptRecorder;

    public RegistrationService(
            UserJpaRepository userJpaRepository,
            EmailOtpJpaRepository emailOtpJpaRepository,
            PasswordEncoder passwordEncoder,
            PasswordPolicyValidator passwordPolicyValidator,
            OtpGenerator otpGenerator,
            OtpEncryptor otpEncryptor,
            OutboxEventWriter outboxEventWriter,
            AuthService authService,
            AppProperties appProperties,
            OtpAttemptRecorder otpAttemptRecorder) {
        this.userJpaRepository = userJpaRepository;
        this.emailOtpJpaRepository = emailOtpJpaRepository;
        this.passwordEncoder = passwordEncoder;
        this.passwordPolicyValidator = passwordPolicyValidator;
        this.otpGenerator = otpGenerator;
        this.otpEncryptor = otpEncryptor;
        this.outboxEventWriter = outboxEventWriter;
        this.authService = authService;
        this.appProperties = appProperties;
        this.otpAttemptRecorder = otpAttemptRecorder;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (userJpaRepository.existsByEmail(email)) {
            log.warn("Registration rejected: email already exists email={}", email);
            throw new UserAlreadyExistsException(email);
        }
        passwordPolicyValidator.validate(request.password());
        Role role = resolveRole(request.role());

        User user = User.register(
                request.firstName().trim(), request.lastName(), email,
                passwordEncoder.encode(request.password()), request.phoneNumber(), role);
        userJpaRepository.save(user);

        if (appProperties.isAutoConfirmEmail()) {
            user.verifyEmail();
            userJpaRepository.save(user);
            log.debug("Auto-confirmed email on registration userId={}", user.getId());
        } else {
            issueVerificationCode(email, user.getId());
        }

        publishRegisteredEvent(user);

        log.debug("User registered userId={} email={}", user.getId(), email);
        return toRegisterResponse(user);
    }

    @Transactional(readOnly = true)
    public boolean verifyEmailOtp(String email, String otp) {
        String normalized = normalize(email);
        Optional<User> byEmail = userJpaRepository.findByEmail(normalized);
        if (byEmail.isEmpty()) {
            throw new InvalidOtpException();
        }
        User user = byEmail.get();
        if (user.isEmailVerified()) {
            throw new InvalidOtpException();
        }

        EmailOtp otpRecord = emailOtpJpaRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(normalized, OtpPurpose.EMAIL_VERIFICATION)
                .orElseThrow(InvalidOtpException::new);

        int maxAttempts = appProperties.getOtp().getMaxAttempts();
        if (!otpRecord.isValid() || otpRecord.isExhausted(maxAttempts)) {
            throw new InvalidOtpException();
        }
        if (!otpRecord.getOtp().equals(otp)) {
            otpAttemptRecorder.recordFailedAttempt(otpRecord.getId());
            log.warn("Failed verification pre-check attempt userId={} attempts={} of {}",
                    user.getId(), otpRecord.getFailedAttempts() + 1, maxAttempts);
            throw new InvalidOtpException();
        }
        return true;
    }

    @Transactional
    public LoginResponse verifyEmail(VerifyEmailRequest request) {
        String email = normalize(request.email());
        Optional<User> byEmail = userJpaRepository.findByEmail(email);
        if (byEmail.isEmpty()) {
            throw new InvalidOtpException();
        }
        User user = byEmail.get();
        if (user.isEmailVerified()) {
            log.debug("Re-verification of already-verified email (idempotent) userId={}", user.getId());
            throw new InvalidOtpException();
        }

        EmailOtp otp = emailOtpJpaRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.EMAIL_VERIFICATION)
                .orElseThrow(InvalidOtpException::new);

        int maxAttempts = appProperties.getOtp().getMaxAttempts();
        if (!otp.isValid() || otp.isExhausted(maxAttempts)) {
            throw new InvalidOtpException();
        }

        if (!otp.getOtp().equals(request.otp())) {
            otpAttemptRecorder.recordFailedAttempt(otp.getId());
            log.warn("Failed email verification attempt userId={} attempts={} of {}",
                    user.getId(), otp.getFailedAttempts() + 1, maxAttempts);
            throw new InvalidOtpException();
        }

        otp.markUsed();
        emailOtpJpaRepository.save(otp);
        user.verifyEmail();
        userJpaRepository.save(user);

        log.debug("Email verified userId={}", user.getId());
        return authService.issueTokenPair(user);
    }

    @Transactional
    public void resendVerificationOtp(ResendVerificationRequest request) {
        String email = normalize(request.email());
        Optional<User> byEmail = userJpaRepository.findByEmail(email);
        if (byEmail.isEmpty()) {
            log.debug("Resend request for unknown email (enum-safe no-op)");
            return;
        }
        User user = byEmail.get();
        if (user.isEmailVerified()) {
            log.debug("Resend request for verified email (enum-safe no-op)");
            return;
        }

        Optional<EmailOtp> latest = emailOtpJpaRepository
                .findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.EMAIL_VERIFICATION);
//        if (latest.isPresent() && withinCooldown(latest.get())) {
//            log.debug("Resend request within cooldown (no new code issued) email={}", email);
//            return;
//        }

        issueVerificationCode(email, user.getId());
        log.warn("New verification code requested userId={}", user.getId());
    }

    private void issueVerificationCode(String email, UUID userId) {
        String code = otpGenerator.generate();
        Instant expiresAt = Instant.now()
                .plus(appProperties.getOtp().getExpirationMinutes(), ChronoUnit.MINUTES);
        emailOtpJpaRepository.revokePendingByEmailAndPurpose(email, OtpPurpose.EMAIL_VERIFICATION);
        EmailOtp otp = EmailOtp.generate(email, OtpPurpose.EMAIL_VERIFICATION, code, expiresAt);
        emailOtpJpaRepository.save(otp);

        if (appProperties.getEmail().isEnabled()) {
            if (otpEncryptor.isConfigured()) {
                outboxEventWriter.writeUserEvent(
                        userId,
                        UserEventTypes.EMAIL_VERIFICATION_REQUESTED,
                        OBJECT_MAPPER.valueToTree(Map.of(
                                "userId", userId.toString(),
                                "email", email,
                                "otp", otpEncryptor.encrypt(code))));
            } else {
                log.error("OTP encryption not configured; email verification delivery skipped userId={}", userId);
            }
        } else {
            log.warn("Email delivery disabled: verification code for {} is {}", email, code);
        }
    }

    private void publishRegisteredEvent(User user) {
        outboxEventWriter.writeUserEvent(
                user.getId(),
                UserEventTypes.REGISTERED,
                OBJECT_MAPPER.valueToTree(Map.of(
                        "userId", user.getId().toString(),
                        "email", user.getEmail(),
                        "role", user.getRole().name(),
                        "createdAt", user.getCreatedAt().toString())));
    }

    private boolean withinCooldown(EmailOtp latest) {
        Duration elapsed = Duration.between(latest.getCreatedAt(), Instant.now());
        return elapsed.getSeconds() < appProperties.getOtp().getResendCooldownSeconds();
    }

    private Role resolveRole(String role) {
        return switch (role) {
            case "BUYER" -> Role.BUYER;
            case "SELLER" -> Role.SELLER;
            default -> throw new ValidationException("Role must be BUYER or SELLER");
        };
    }

    private RegisterResponse toRegisterResponse(User user) {
        return new RegisterResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.isEnabled(),
                user.getCreatedAt());
    }

    private String normalize(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
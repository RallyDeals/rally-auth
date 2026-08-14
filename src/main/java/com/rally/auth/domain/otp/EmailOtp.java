package com.rally.auth.domain.otp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_otps")
@Getter
@NoArgsConstructor
public class EmailOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private OtpPurpose purpose;

    @Column(name = "otp", nullable = false, length = 6)
    private String otp;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public static EmailOtp generate(String email, OtpPurpose purpose, String otp, Instant expiresAt) {
        EmailOtp emailOtp = new EmailOtp();
        emailOtp.email = email;
        emailOtp.purpose = purpose;
        emailOtp.otp = otp;
        emailOtp.expiresAt = expiresAt;
        return emailOtp;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return usedAt == null && !isExpired();
    }

    public void markUsed() {
        if (usedAt == null) {
            usedAt = Instant.now();
        }
    }

    public void incrementFailedAttempts() {
        this.failedAttempts++;
    }

    public boolean isExhausted(int maxAttempts) {
        return failedAttempts >= maxAttempts;
    }
}
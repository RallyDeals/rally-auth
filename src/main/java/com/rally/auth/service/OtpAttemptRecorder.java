package com.rally.auth.service;

import com.rally.auth.repository.EmailOtpJpaRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OtpAttemptRecorder {

    private final EmailOtpJpaRepository emailOtpJpaRepository;

    public OtpAttemptRecorder(EmailOtpJpaRepository emailOtpJpaRepository) {
        this.emailOtpJpaRepository = emailOtpJpaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(UUID otpId) {
        emailOtpJpaRepository.findById(otpId).ifPresent(otp -> {
            otp.incrementFailedAttempts();
            emailOtpJpaRepository.save(otp);
        });
    }
}
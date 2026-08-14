package com.rally.auth.repository;

import com.rally.auth.domain.otp.EmailOtp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailOtpJpaRepository extends JpaRepository<EmailOtp, UUID> {

    Optional<EmailOtp> findFirstByEmailAndPurposeOrderByCreatedAtDesc(
            String email, com.rally.auth.domain.otp.OtpPurpose purpose);
}
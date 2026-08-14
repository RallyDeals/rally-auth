package com.rally.auth.repository;

import com.rally.auth.domain.otp.EmailOtp;
import com.rally.auth.domain.otp.OtpPurpose;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailOtpJpaRepository extends JpaRepository<EmailOtp, UUID> {

    Optional<EmailOtp> findFirstByEmailAndPurposeOrderByCreatedAtDesc(
            String email, OtpPurpose purpose);

    /**
     * Revokes all unused, unexpired codes for the email+purpose (bulk
     * {@code used_at = now}) so only the newest code remains usable.
     */
    @Modifying
    @Query("UPDATE EmailOtp o SET o.usedAt = CURRENT_TIMESTAMP "
            + "WHERE o.email = :email AND o.purpose = :purpose AND o.usedAt IS NULL")
    int revokePendingByEmailAndPurpose(@Param("email") String email, @Param("purpose") OtpPurpose purpose);
}
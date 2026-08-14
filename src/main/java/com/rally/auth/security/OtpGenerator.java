package com.rally.auth.security;

import com.rally.auth.config.AppProperties;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Cryptographically secure n-digit OTP generator (research R-003).
 * Length comes from {@code app.otp.length}; leading zeros are allowed.
 */
@Component
public class OtpGenerator {

    private static final char[] DIGITS = "0123456789".toCharArray();

    private final int length;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpGenerator(AppProperties appProperties) {
        this.length = appProperties.getOtp().getLength();
    }

    public String generate() {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(DIGITS[secureRandom.nextInt(DIGITS.length)]);
        }
        return sb.toString();
    }
}
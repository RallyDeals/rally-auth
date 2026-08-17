package com.rally.auth.security;

import com.rally.auth.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * JDK AES-256-GCM encryptor for OTP codes delivered through the event stream
 * (research R-007). The 256-bit key is derived once from
 * {@code app.otp.encryption.password} + {@code app.otp.encryption.salt}.
 * A fresh 12-byte IV is prepended to the ciphertext and the whole blob is
 * Base64-encoded. Blank/missing secret ⇒ not configured (fail-safe, FR-020).
 */
@Component
public class OtpEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int IV_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int KEY_BITS = 256;
    private static final int PBKDF2_ITERATIONS = 100_000;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKey key;
    private final boolean configured;

    public OtpEncryptor(AppProperties appProperties) {
        String password = appProperties.getOtp().getEncryption().getPassword();
        String salt = appProperties.getOtp().getEncryption().getSalt();
        if (isBlank(password) || isBlank(salt)) {
            this.key = null;
            this.configured = false;
            return;
        }
        this.key = deriveKey(password, salt);
        this.configured = true;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String encrypt(String plainCode) {
        if (!configured) {
            throw new IllegalStateException("OTP encryption not configured");
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plainCode.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt OTP", e);
        }
    }

    private static SecretKey deriveKey(String password, String salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_ALGORITHM);
            KeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8),
                    PBKDF2_ITERATIONS,
                    KEY_BITS);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to derive OTP encryption key", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
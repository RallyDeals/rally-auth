package com.rally.auth.security;

import com.rally.auth.config.AppProperties;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

@Component
public class OtpEncryptor {

    private final TextEncryptor encryptor;
    private final boolean configured;

    public OtpEncryptor(AppProperties appProperties) {
        String password = appProperties.getOtp().getEncryption().getPassword();
        String salt = appProperties.getOtp().getEncryption().getSalt();
        if (isBlank(password) || isBlank(salt)) {
            this.encryptor = null;
            this.configured = false;
            return;
        }
        this.encryptor = Encryptors.text(password, salt);
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
            return encryptor.encrypt(plainCode);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt OTP", e);
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
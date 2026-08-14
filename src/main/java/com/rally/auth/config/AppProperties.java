package com.rally.auth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private boolean autoConfirmEmail = false;

    private final Email email = new Email();

    private final Otp otp = new Otp();

    private final Security security = new Security();

    private final Password password = new Password();

    @Getter
    @Setter
    public static class Email {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Otp {
        private int length = 6;
        private int expirationMinutes = 10;
        private int maxAttempts = 5;
        private int resendCooldownSeconds = 60;
    }

    @Getter
    @Setter
    public static class Security {
        private int accessTokenSeconds = 900;
        private int refreshTokenDays = 30;

        private final Jwt jwt = new Jwt();

        @Getter
        @Setter
        public static class Jwt {
            private String privateKey;
            private String publicKey;
        }
    }

    @Getter
    @Setter
    public static class Password {
        private int minLength = 8;
        private int maxLength = 64;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireDigit = true;
        private boolean requireSpecial = true;
    }
}
package com.rally.auth.security;

import com.rally.auth.domain.user.Role;
import com.rally.auth.domain.user.User;
import io.jsonwebtoken.Jwts;
import java.security.PrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenService {

    private final PrivateKey privateKey;
    private final int accessTokenSeconds;

    public JwtTokenService(
            PrivateKey privateKey,
            @Value("${app.security.access-token-seconds}") int accessTokenSeconds) {
        this.privateKey = privateKey;
        this.accessTokenSeconds = accessTokenSeconds;
    }

    public String sign(User user, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("firstName", user.getFirstName())
                .claim("id", user.getId())
                .claim("lastName", user.getLastName())
                .claim("email", user.getEmail())
                .claim("phoneNumber", user.getPhoneNumber())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenSeconds)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }
}
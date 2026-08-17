package com.rally.auth.domain.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "phone_number", length = 30)
    private String phoneNumber;

    @Column(name = "profile_picture", length = 1000)
    private String profilePicture;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 10)
    private Role role;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static User register(
            String firstName, String lastName, String email,
            String passwordHash, String phoneNumber, Role role) {
        User user = new User();
        user.firstName = firstName;
        user.lastName = lastName;
        user.email = email;
        user.passwordHash = passwordHash;
        user.phoneNumber = phoneNumber;
        user.role = role == null ? Role.BUYER : role;
        user.enabled = true;
        user.emailVerified = false;
        return user;
    }

    public void verifyEmail() {
        if (!emailVerified) {
            emailVerified = true;
            emailVerifiedAt = Instant.now();
        }
    }

    public void updateProfile(String firstName, String lastName, String phoneNumber) {
        if (firstName != null) {
            this.firstName = firstName;
        }
        if (lastName != null) {
            this.lastName = lastName;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber;
        }
    }

    public void applyProfile(String firstName, String lastName, String phoneNumber) {
        if (firstName != null) {
            this.firstName = firstName;
        }
        if (lastName != null) {
            this.lastName = lastName.isEmpty() ? null : lastName;
        }
        if (phoneNumber != null) {
            this.phoneNumber = phoneNumber.isEmpty() ? null : phoneNumber;
        }
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }

    public void changeRole(Role role) {
        this.role = role;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    public void softDelete() {
        this.enabled = false;
        this.firstName = null;
        this.lastName = null;
        this.phoneNumber = null;
        this.email = "deleted-" + UUID.randomUUID() + "@deleted.rally";
    }
}
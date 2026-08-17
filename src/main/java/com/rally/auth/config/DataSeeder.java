package com.rally.auth.config;

import com.rally.auth.domain.user.Role;
import com.rally.auth.domain.user.User;
import com.rally.auth.repository.UserJpaRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private static final String AVATAR_URL =
            "https://png.pngtree.com/png-clipart/20230927/original/pngtree-man-avatar-image-for-profile-png-image_13001882.png";

    private final UserJpaRepository userJpaRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserJpaRepository userJpaRepository, PasswordEncoder passwordEncoder) {
        this.userJpaRepository = userJpaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String hash = passwordEncoder.encode("SeedPass123!");
        List<User> users = List.of(
                seed("Admin", "Active", "admin.active@rally.local", hash, Role.ADMIN, true, true),
                seed("Admin", "Locked", "admin.locked@rally.local", hash, Role.ADMIN, false, true),
                seed("Admin", "Pending", "admin.pending@rally.local", hash, Role.ADMIN, true, false),
                seed("Seller", "Active", "seller.active@rally.local", hash, Role.SELLER, true, true),
                seed("Seller", "Locked", "seller.locked@rally.local", hash, Role.SELLER, false, true),
                seed("Seller", "Pending", "seller.pending@rally.local", hash, Role.SELLER, true, false),
                seed("Buyer", "Active", "buyer.active@rally.local", hash, Role.BUYER, true, true),
                seed("Buyer", "Locked", "buyer.locked@rally.local", hash, Role.BUYER, false, true),
                seed("Buyer", "Pending", "buyer.pending@rally.local", hash, Role.BUYER, true, false),
                seed("Buyer", "Suspended", "buyer.suspended@rally.local", hash, Role.BUYER, false, false));
        List<User> toSave = users.stream()
                .filter(user -> !userJpaRepository.existsByEmail(user.getEmail()))
                .toList();
        userJpaRepository.saveAll(toSave);
        if (!toSave.isEmpty()) {
            log.info("Seeded {} dev users (3 ADMIN, 3 SELLER, 4 BUYER) covering active/locked/pending/suspended statuses - password SeedPass123!",
                    toSave.size());
        }
    }

    private static User seed(
            String firstName, String lastName, String email,
            String passwordHash, Role role, boolean enabled, boolean emailVerified) {
        User user = User.register(firstName, lastName, email, passwordHash, null, role);
        user.setProfilePicture(AVATAR_URL);
        if (emailVerified) {
            user.verifyEmail();
        }
        if (!enabled) {
            user.disable();
        }
        return user;
    }
}
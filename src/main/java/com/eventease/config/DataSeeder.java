package com.eventease.config;

import com.eventease.entity.User;
import com.eventease.enums.Role;
import com.eventease.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DataSeeder runs once at application startup.
 * Creates a default ADMIN user if no admin account exists in the database.
 *
 * Default Admin Credentials:
 *   Email   : admin@eventease.com
 *   Password: Admin@1234
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private static final String ADMIN_EMAIL    = "admin@eventease.com";
    private static final String ADMIN_NAME     = "EventEase Admin";
    private static final String ADMIN_PASSWORD = "Admin@1234";

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            log.info("DataSeeder: Admin user already exists — skipping seed.");
            return;
        }

        User admin = User.builder()
                .name(ADMIN_NAME)
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);
        log.info("DataSeeder: Default ADMIN user created successfully.");
        log.info("  Email   : {}", ADMIN_EMAIL);
        log.info("  Password: {}", ADMIN_PASSWORD);
    }
}

package com.pos.auth.config;

import com.pos.auth.entity.User;
import com.pos.auth.enums.Role;
import com.pos.auth.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrapRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.bootstrap.email:}")
    private String bootstrapEmail;

    @Value("${admin.bootstrap.password:}")
    private String bootstrapPassword;

    @Override
    public void run(String... args) {

        if (bootstrapEmail == null || bootstrapEmail.isBlank()
                || bootstrapPassword == null || bootstrapPassword.isBlank()) {

            log.info(
                    "ADMIN_EMAIL/ADMIN_PASSWORD not configured; skipping admin bootstrap."
            );
            return;
        }

        if (userRepository.existsByRole(Role.ROLE_ADMIN)) {

            log.info(
                    "An admin user already exists; skipping admin bootstrap."
            );
            return;
        }

        if (userRepository.existsByEmail(bootstrapEmail)) {

            log.warn(
                    "ADMIN_EMAIL {} is already registered to a non-admin user; " +
                            "skipping bootstrap. Promote the account manually if needed.",
                    bootstrapEmail
            );
            return;
        }

        User admin = User.builder()
                .name("Administrator")
                .email(bootstrapEmail)
                .password(passwordEncoder.encode(bootstrapPassword))
                .role(Role.ROLE_ADMIN)
                .build();

        userRepository.save(admin);

        log.info(
                "Bootstrap admin account created for {}",
                bootstrapEmail
        );
    }
}

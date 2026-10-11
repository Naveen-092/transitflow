package com.naveen.transitflow.config;

import com.naveen.transitflow.entity.Role;
import com.naveen.transitflow.entity.User;
import com.naveen.transitflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the first ADMIN account on startup if it does not exist yet. */
@Component
public class AdminDataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminDataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminName;
    private final String adminEmail;
    private final String adminPhone;
    private final String adminPassword;

    public AdminDataInitializer(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin.name}") String adminName,
                                @Value("${app.admin.email}") String adminEmail,
                                @Value("${app.admin.phone}") String adminPhone,
                                @Value("${app.admin.password}") String adminPassword) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminName = adminName;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPhone = adminPhone;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin account already exists: {}", adminEmail);
            return;
        }
        User admin = new User(adminName, adminEmail, passwordEncoder.encode(adminPassword), adminPhone, Role.ADMIN);
        userRepository.save(admin);
        log.info("Default admin account created: {}", adminEmail);
    }
}
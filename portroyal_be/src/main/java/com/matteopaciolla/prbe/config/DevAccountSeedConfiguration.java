package com.matteopaciolla.prbe.config;

import com.matteopaciolla.prbe.constants.enums.UserRole;
import com.matteopaciolla.prbe.model.entity.UserEntity;
import com.matteopaciolla.prbe.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@Slf4j
@Configuration
@Profile("dev")
public class DevAccountSeedConfiguration {

    private static final String ADMIN_USERNAME = "admin";

    @Bean
    CommandLineRunner developmentAccountSeeder(
            PasswordEncoder passwordEncoder,
            UserRepository userRepository,
            @Value("${dev.seed.admin-password:}") String adminPassword) {
        return args -> {
            log.info("Application started successfully");
            seedAccount(userRepository, passwordEncoder, ADMIN_USERNAME, adminPassword,
                    List.of(UserRole.ADMIN, UserRole.USER));
        };
    }

    private static void seedAccount(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            String username,
            String password,
            List<UserRole> roles) {
        if (userRepository.findByUsername(username).isPresent()) {
            log.debug("Development seed account is already configured");
            return;
        }
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("A non-empty development seed password is required for each new seed account");
        }

        UserEntity user = new UserEntity(username, passwordEncoder.encode(password), roles);
        user.setEnabled(true);
        user.setEmailConfirmed(true);
        userRepository.save(user);
        log.info("Created a development seed account");
    }
}

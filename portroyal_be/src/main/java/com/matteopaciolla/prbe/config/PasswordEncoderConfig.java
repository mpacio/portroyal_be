package com.matteopaciolla.prbe.config;

import com.matteopaciolla.prbe.security.MigratingPasswordEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordEncoderConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return new MigratingPasswordEncoder();
    }
}

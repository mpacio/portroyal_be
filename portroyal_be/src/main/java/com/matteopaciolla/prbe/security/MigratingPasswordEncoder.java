package com.matteopaciolla.prbe.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public final class MigratingPasswordEncoder implements PasswordEncoder {

    private static final String ARGON2_PREFIX = "{argon2}";
    private static final String BCRYPT_PREFIX = "{bcrypt}";

    private final PasswordEncoder argon2 = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    private final PasswordEncoder bcrypt = new BCryptPasswordEncoder();

    @Override
    public String encode(CharSequence rawPassword) {
        return ARGON2_PREFIX + argon2.encode(rawPassword);
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (encodedPassword == null) {
            return false;
        }
        if (encodedPassword.startsWith(ARGON2_PREFIX)) {
            return argon2.matches(rawPassword, encodedPassword.substring(ARGON2_PREFIX.length()));
        }
        if (encodedPassword.startsWith(BCRYPT_PREFIX)) {
            return bcrypt.matches(rawPassword, encodedPassword.substring(BCRYPT_PREFIX.length()));
        }
        if (encodedPassword.startsWith("$2")) {
            return bcrypt.matches(rawPassword, encodedPassword);
        }
        return false;
    }

    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        if (encodedPassword == null || !encodedPassword.startsWith(ARGON2_PREFIX)) {
            return true;
        }
        return argon2.upgradeEncoding(encodedPassword.substring(ARGON2_PREFIX.length()));
    }
}

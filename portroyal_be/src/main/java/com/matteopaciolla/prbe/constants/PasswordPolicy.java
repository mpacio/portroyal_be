package com.matteopaciolla.prbe.constants;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {

    public static final int MIN_CODE_POINTS = 8;
    public static final int MAX_UTF8_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean isValidNewPassword(String password) {
        return password != null
                && !password.isBlank()
                && password.codePointCount(0, password.length()) >= MIN_CODE_POINTS
                && password.getBytes(StandardCharsets.UTF_8).length <= MAX_UTF8_BYTES;
    }
}

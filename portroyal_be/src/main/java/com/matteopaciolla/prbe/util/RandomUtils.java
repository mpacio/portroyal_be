package com.matteopaciolla.prbe.util;

import java.util.Random;

public class RandomUtils {

    private final static Random random = new Random();

    public static String get6DigitRandomCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }

    public static String get4DigitRandomCode() {
        return String.format("%04d", (int) (Math.random() * 10000));
    }

    public static String getNDigitRandomCode(int digits) {
        if (digits < 1) {
            throw new IllegalArgumentException("Number of digits must be greater than 0");
        }
        return random.ints(digits, 0, 10)
                .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
    }

    public static String getRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int randomIndex = random.nextInt(chars.length());
            password.append(chars.charAt(randomIndex));
        }
        return password.toString();
    }
}

package com.matteopaciolla.prbe.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class Base64StringUtilsTest {

    @Test
    void getBase64UrlUuid_shouldReturnBase64UrlString() {
        String result = Base64StringUtils.getBase64UrlUuid();
        assertTrue(result.matches("^[0-9a-zA-Z_-]+$"));
    }

}
package com.matteopaciolla.prbe.security.callback;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallbackSignatureTest {

    @Test
    void signsTimestampEventIdAndExactBodyBytesWithVersionedHmacSha256() throws Exception {
        byte[] body = "{\"a\":1}".getBytes(StandardCharsets.UTF_8);

        String signature = CallbackSignature.sign(
                "0123456789abcdef0123456789abcdef", "1700000000", "event-123", body);

        assertThat(signature)
                .isEqualTo("v1=79b4000e90ac0f7f4027b2b9abc3940a5bc1015aa89a1721bd327d186cf3ab36");
    }

    @Test
    void refusesShortSigningSecret() {
        assertThatThrownBy(() -> CallbackSignature.sign("short", "1700000000", "event-123", new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

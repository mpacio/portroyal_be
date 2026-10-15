package com.matteopaciolla.prbe.security.callback;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CallbackSecretCryptoTest {

    private static final String KEY = Base64.getEncoder().encodeToString(
            "0123456789abcdef0123456789abcdef".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

    @Test
    void encryptsAndDecryptsWithAuthenticatedCiphertext() {
        CallbackSecretCrypto crypto = new CallbackSecretCrypto(KEY);

        String ciphertext = crypto.encrypt("a-long-callback-secret-that-is-not-stored-as-plaintext");

        assertThat(ciphertext).startsWith("v1:").doesNotContain("a-long-callback-secret");
        assertThat(crypto.decrypt(ciphertext))
                .isEqualTo("a-long-callback-secret-that-is-not-stored-as-plaintext");
    }

    @Test
    void rejectsCiphertextModifiedOrCreatedWithDifferentKey() {
        CallbackSecretCrypto writer = new CallbackSecretCrypto(KEY);
        CallbackSecretCrypto otherKey = new CallbackSecretCrypto(
                Base64.getEncoder().encodeToString(
                        "abcdef0123456789abcdef0123456789".getBytes(java.nio.charset.StandardCharsets.US_ASCII)));

        String ciphertext = writer.encrypt("callback secret");

        assertThatThrownBy(() -> otherKey.decrypt(ciphertext))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void refusesMissingOrIncorrectLengthEncryptionKeys() {
        CallbackSecretCrypto missingKey = new CallbackSecretCrypto("");
        assertThatThrownBy(() -> missingKey.encrypt("callback secret"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CallbackSecretCrypto(
                Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void decryptsPreviousKeyAndReEncryptsForTheActiveKey() {
        String oldKey = KEY;
        String newKey = Base64.getEncoder().encodeToString(
                "abcdef0123456789abcdef0123456789".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        CallbackSecretCrypto oldCrypto = new CallbackSecretCrypto(oldKey, "old", "");
        CallbackSecretCrypto rotatingCrypto = new CallbackSecretCrypto(newKey, "new", "old=" + oldKey);

        String oldCiphertext = oldCrypto.encrypt("webhook secret");
        assertThat(rotatingCrypto.needsReEncryption(oldCiphertext)).isTrue();
        assertThat(rotatingCrypto.decrypt(oldCiphertext)).isEqualTo("webhook secret");

        String newCiphertext = rotatingCrypto.encrypt(rotatingCrypto.decrypt(oldCiphertext));
        assertThat(newCiphertext).startsWith("v1:new:");
        assertThat(rotatingCrypto.needsReEncryption(newCiphertext)).isFalse();
        assertThat(rotatingCrypto.decrypt(newCiphertext)).isEqualTo("webhook secret");
    }
}

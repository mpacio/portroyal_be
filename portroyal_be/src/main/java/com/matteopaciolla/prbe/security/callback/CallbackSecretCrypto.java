package com.matteopaciolla.prbe.security.callback;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class CallbackSecretCrypto {

    private static final String PREFIX = "v1:";
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final Pattern KEY_ID_PATTERN = Pattern.compile("[A-Za-z0-9_-]{1,32}");

    private final String activeKeyId;
    private final Map<String, SecretKeySpec> keys;
    private final boolean activeKeyConfigured;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public CallbackSecretCrypto(
            @Value("${pr.sentinel.callback-encryption-key:}") String activeKey,
            @Value("${pr.sentinel.callback-encryption-key-id:current}") String activeKeyId,
            @Value("${pr.sentinel.callback-previous-encryption-keys:}") String previousKeys) {
        this.activeKeyId = requireKeyId(activeKeyId);
        Map<String, SecretKeySpec> configuredKeys = new HashMap<>();
        this.activeKeyConfigured = activeKey != null && !activeKey.isBlank();
        if (activeKeyConfigured) {
            configuredKeys.put(this.activeKeyId, parseKey(activeKey));
        }
        if (previousKeys != null && !previousKeys.isBlank()) {
            for (String entry : previousKeys.split(",")) {
                String[] pair = entry.trim().split("=", 2);
                if (pair.length != 2) {
                    throw new IllegalStateException(
                            "Previous callback encryption keys must use the format key-id=base64-key.");
                }
                String keyId = requireKeyId(pair[0].trim());
                if (configuredKeys.putIfAbsent(keyId, parseKey(pair[1].trim())) != null) {
                    throw new IllegalStateException("Callback encryption key IDs must be unique.");
                }
            }
        }
        this.keys = Map.copyOf(configuredKeys);
    }

    CallbackSecretCrypto(String activeKey) {
        this(activeKey, "current", "");
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) {
            throw new IllegalArgumentException("A non-blank callback secret is required.");
        }
        SecretKeySpec key = keys.get(activeKeyId);
        if (!activeKeyConfigured || key == null) {
            throw new IllegalStateException("PORTROYAL_CALLBACK_ENCRYPTION_KEY must be configured.");
        }
        byte[] nonce = new byte[NONCE_LENGTH];
        secureRandom.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            byte[] payload = new byte[nonce.length + encrypted.length];
            System.arraycopy(nonce, 0, payload, 0, nonce.length);
            System.arraycopy(encrypted, 0, payload, nonce.length, encrypted.length);
            return PREFIX + activeKeyId + ":" + Base64.getEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt callback secret.", exception);
        }
    }

    public String decrypt(String encryptedSecret) {
        if (encryptedSecret == null || !encryptedSecret.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Callback secret is not encrypted with the supported format.");
        }
        String encodedPayload = encryptedSecret.substring(PREFIX.length());
        int separator = encodedPayload.indexOf(':');
        String keyId = separator < 0 ? activeKeyId : requireKeyId(encodedPayload.substring(0, separator));
        if (separator >= 0) {
            encodedPayload = encodedPayload.substring(separator + 1);
        }
        SecretKeySpec key = keys.get(keyId);
        if (key == null) {
            throw new IllegalStateException("The required callback encryption key is not configured.");
        }
        try {
            byte[] payload = Base64.getDecoder().decode(encodedPayload);
            if (payload.length <= NONCE_LENGTH) {
                throw new IllegalArgumentException("Callback secret ciphertext is malformed.");
            }
            byte[] nonce = Arrays.copyOfRange(payload, 0, NONCE_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(payload, NONCE_LENGTH, payload.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(ciphertext), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt callback secret.", exception);
        }
    }

    public boolean needsReEncryption(String encryptedSecret) {
        return encryptedSecret == null || !encryptedSecret.startsWith(PREFIX + activeKeyId + ":");
    }

    private static SecretKeySpec parseKey(String encodedKey) {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Callback encryption keys must be Base64-encoded.", exception);
        }
        if (keyBytes.length != 32) {
            Arrays.fill(keyBytes, (byte) 0);
            throw new IllegalStateException("Callback encryption keys must decode to exactly 32 bytes.");
        }
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        Arrays.fill(keyBytes, (byte) 0);
        return key;
    }

    private static String requireKeyId(String keyId) {
        if (keyId == null || !KEY_ID_PATTERN.matcher(keyId).matches()) {
            throw new IllegalStateException("Callback encryption key IDs must use 1-32 letters, digits, '_' or '-'.");
        }
        return keyId;
    }
}

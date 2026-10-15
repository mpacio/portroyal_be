package com.matteopaciolla.prbe.security.callback;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

public final class CallbackSignature {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String SIGNATURE_VERSION = "v1=";

    private CallbackSignature() {
    }

    public static String sign(String secret, String timestamp, String eventId, byte[] body) throws GeneralSecurityException {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("Callback signing secret must contain at least 32 bytes.");
        }
        if (timestamp == null || timestamp.isBlank() || eventId == null || eventId.isBlank()) {
            throw new IllegalArgumentException("A timestamp and event id are required to sign callbacks.");
        }
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
        mac.update(timestamp.getBytes(StandardCharsets.US_ASCII));
        mac.update((byte) '.');
        mac.update(eventId.getBytes(StandardCharsets.US_ASCII));
        mac.update((byte) '.');
        mac.update(body);
        return SIGNATURE_VERSION + HexFormat.of().formatHex(mac.doFinal());
    }
}

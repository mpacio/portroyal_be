package com.matteopaciolla.prbe.util;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public class Base64StringUtils {

    // Generate a UUID in Base64url string that matches this pattern ^[0-9a-zA-Z_-]+$
    public static String getBase64UrlUuid() {
        // Generate a UUID
        UUID uuid = UUID.randomUUID();

        // Convert UUID to byte array
        byte[] uuidBytes = ByteBuffer.wrap(new byte[16])
                .putLong(uuid.getMostSignificantBits())
                .putLong(uuid.getLeastSignificantBits())
                .array();

        // Encode the byte array to Base64
        return Base64.getUrlEncoder().withoutPadding().encodeToString(uuidBytes);
    }
}

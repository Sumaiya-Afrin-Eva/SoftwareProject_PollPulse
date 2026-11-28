package com.pollpulse.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 helper shared by the audit chain and, later, the vote chain. Keeping a
 * single implementation guarantees both chains use exactly the same digest.
 */
public final class HashUtil {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private HashUtil() {
    }

    /**
     * Returns the SHA-256 digest of {@code input} as 64 lower-case hexadecimal
     * characters.
     */
    public static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            char[] hex = new char[bytes.length * 2];
            for (int index = 0; index < bytes.length; index++) {
                int value = bytes[index] & 0xFF;
                hex[index * 2] = HEX[value >>> 4];
                hex[index * 2 + 1] = HEX[value & 0x0F];
            }
            return new String(hex);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available in this JVM", exception);
        }
    }
}

package com.secureteam.auth;

import jakarta.enterprise.context.ApplicationScoped;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class PasswordService {

    private static final int ITERATIONS = 100_000;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();

    public HashResult hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("password is null");
        }

        byte[] salt = new byte[SALT_LENGTH_BYTES];
        secureRandom.nextBytes(salt);

        byte[] derived = pbkdf2(password.toCharArray(), salt);

        HashResult r = new HashResult();
        r.hashBase64 = Base64.getEncoder().encodeToString(derived);
        r.saltBase64 = Base64.getEncoder().encodeToString(salt);
        return r;
    }

    public boolean verify(String password, String saltBase64, String expectedHashBase64) {
        if (password == null || saltBase64 == null || expectedHashBase64 == null) return false;

        byte[] salt = Base64.getDecoder().decode(saltBase64);
        byte[] derived = pbkdf2(password.toCharArray(), salt);

        byte[] expected = Base64.getDecoder().decode(expectedHashBase64);
        return constantTimeEquals(derived, expected);
    }

    private byte[] pbkdf2(char[] password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS);
            SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing failed", e);
        }
    }

    private boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a == null || b == null || a.length != b.length) return false;
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }

    public static class HashResult {
        public String hashBase64;
        public String saltBase64;
    }
}

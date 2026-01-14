package com.secureteam.auth;

import org.jboss.aerogear.security.otp.api.Base32;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests TOTP verification with known RFC 6238 test vectors
 */
class TotpTestHelper {
    
    /**
     * Test TOTP generation with RFC 6238 test vector
     * Secret: JBSWY3DPEBLW64TMMQ======
     * Time: 59 seconds = T = 1
     * Expected code: 287082
     */
    @Test
    void testTotpAlgorithm() throws Exception {
        String secret = "JBSWY3DPEBLW64TMMQ======";
        long testTime = 59; // seconds
        long timeWindow = 30;
        long t = testTime / timeWindow; // Should be 1
        
        byte[] key = Base32.decode(secret);
        String code = generateTotpCode(key, t);
        
        System.out.println("RFC 6238 Test Vector:");
        System.out.println("Secret: " + secret);
        System.out.println("Time: " + testTime + "s");
        System.out.println("T value: " + t);
        System.out.println("Generated code: " + code);
        System.out.println("Expected: 287082");
        
        assertEquals("287082", code, "TOTP code should match RFC 6238 test vector");
    }
    
    private static String generateTotpCode(byte[] key, long t) throws Exception {
        byte[] data = new byte[8];
        long value = t;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        javax.crypto.spec.SecretKeySpec signKey = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        // RFC 6238: offset = last 4 bits of hash[19]
        int offset = hash[hash.length - 1] & 0x0f;
        System.out.println("Offset: " + offset);
        
        // Dynamic truncation per RFC 6238
        // P = hash[offset] || hash[offset+1] || hash[offset+2] || hash[offset+3]
        int p = ((hash[offset] & 0xff) << 24)
              | ((hash[offset + 1] & 0xff) << 16)
              | ((hash[offset + 2] & 0xff) << 8)
              | (hash[offset + 3] & 0xff);
        
        System.out.println("P (with sign): " + p);
        
        // Mask off the sign bit
        p = p & 0x7fffffff;
        System.out.println("P (without sign): " + p);
        
        // Get 6 digit code
        int code_value = p % 1000000;
        System.out.println("Code value: " + code_value);

        return String.format("%06d", code_value);
    }
}


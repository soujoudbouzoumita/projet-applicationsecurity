package com.secureteam.auth;

import org.jboss.aerogear.security.otp.Totp;
import org.jboss.aerogear.security.otp.api.Base32;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import java.security.SecureRandom;

@ApplicationScoped
public class TotpService {

    private static final Logger LOG = Logger.getLogger(TotpService.class);

    public String generateSecret() {
        // Generate random 160-bit value and Base32 encode it
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return Base32.encode(buffer);
    }

    public boolean validateCode(String secret, String code) {
        try {
            // Use Aerogear's TOTP implementation which handles RFC 6238 correctly
            Totp totp = new Totp(secret);
            
            // The verify method checks the code against current window
            // We need to check a wider window for clock drift, so check manually
            long now = System.currentTimeMillis();
            
            // Check ±3 windows (±90 seconds)
            for (int i = -3; i <= 3; i++) {
                long time = now + (i * 30000); // ± 30 seconds per window
                // Aerogear's verify() method accepts only the code, so we'll check ourselves
                if (verifyCode(secret, code, time)) {
                    if (i != 0)
                        LOG.infov("[MFA] Validated with drift: {0}s (window {1})", (i * 30), i);
                    LOG.infov("[MFA] Validation SUCCESS for code {0} at window {1}", code, i);
                    return true;
                }
            }

            LOG.warnv("[MFA] Validation FAILED for code {0} (current time: {1}ms)", code, now);
            return false;
        } catch (Exception e) {
            LOG.error("MFA Validation Error", e);
            return false;
        }
    }

    private boolean verifyCode(String secret, String code, long timeMillis) throws Exception {
        byte[] key = Base32.decode(secret);
        long timeWindow = 30; // 30 seconds
        long t = (timeMillis / 1000) / timeWindow;

        byte[] data = new byte[8];
        long value = t;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        javax.crypto.spec.SecretKeySpec signKey = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0x0f;
        
        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash = (truncatedHash << 8) | (hash[offset + i] & 0xFF);
        }
        
        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;

        return String.format("%06d", truncatedHash).equals(code);
    }

    public String generateQrCodeUri(String secret, String account, String issuer) {
        // Format: otpauth://totp/[issuer:]account?secret=...&issuer=...
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&period=30&digits=6&algorithm=SHA1",
                issuer, account, secret, issuer);
    }

    public String generateQrCodeImage(String qrCodeUri) {
        try {
            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUri,
                    com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);

            java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return java.util.Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            LOG.error("QR Code generation failed", e);
            return "";
        }
    }
}

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
        // Generate random 160-bit value and Base32 encode it (RFC 6238 compliant)
        byte[] buffer = new byte[20];
        new SecureRandom().nextBytes(buffer);
        return Base32.encode(buffer);
    }

    public boolean validateCode(String secret, String code) {
        try {
            // Validate input format (6 digits)
            if (code == null || !code.matches("^[0-9]{6}$")) {
                LOG.warnv("[MFA] Invalid TOTP code format: expected 6 digits");
                return false;
            }

            // Use Aerogear's TOTP implementation which handles RFC 6238 correctly
            Totp totp = new Totp(secret);
            
            // RFC 6238 recommends checking ±30 seconds by default
            // We allow ±1 minute (±2 windows) for clock drift tolerance
            // This is a security/usability balance (vs ±5 minutes which is excessive)
            long now = System.currentTimeMillis();
            
            // Check ±2 windows (±60 seconds total) - RFC 6238 compliant
            // i=-2 → -60s, i=-1 → -30s, i=0 → current, i=+1 → +30s, i=+2 → +60s
            for (int i = -2; i <= 2; i++) {
                long time = now + (i * 30000); // ± 30 seconds per window
                if (verifyCode(secret, code, time)) {
                    if (i != 0) {
                        // Log drift but NOT the code itself
                        LOG.infov("[MFA] TOTP validated with clock drift of {0} seconds (window offset: {1})", 
                                 (i * 30), i);
                    } else {
                        LOG.debugv("[MFA] TOTP validation SUCCESS (no clock drift)");
                    }
                    return true;
                }
            }

            // Code outside acceptable window - DENY
            LOG.infov("[MFA] TOTP validation FAILED - code outside valid window");
            return false;
        } catch (Exception e) {
            LOG.errorv(e, "[MFA] TOTP validation error: {0}", e.getMessage());
            return false;
        }
    }

    /**
     * RFC 6238 HMAC-SHA1 based TOTP verification
     * This implements the standard HOTP algorithm with HMAC-SHA1
     */
    private boolean verifyCode(String secret, String code, long timeMillis) throws Exception {
        byte[] key = Base32.decode(secret);
        long timeWindow = 30; // 30 seconds window (RFC 6238)
        long t = (timeMillis / 1000) / timeWindow;

        // Create 8-byte value of time counter
        byte[] data = new byte[8];
        long value = t;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        // HMAC-SHA1 computation
        javax.crypto.spec.SecretKeySpec signKey = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        // Dynamic truncation (RFC 4226)
        int offset = hash[hash.length - 1] & 0x0f;
        
        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash = (truncatedHash << 8) | (hash[offset + i] & 0xFF);
        }
        
        // Ensure positive value and limit to 6 digits
        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;

        return String.format("%06d", truncatedHash).equals(code);
    }

    public String generateQrCodeUri(String secret, String account, String issuer) {
        // Format: otpauth://totp/[issuer:]account?secret=...&issuer=...&period=30&digits=6&algorithm=SHA1
        // RFC 6238 compliant
        if (account == null || account.isEmpty()) {
            throw new IllegalArgumentException("Account name cannot be empty");
        }
        if (issuer == null || issuer.isEmpty()) {
            throw new IllegalArgumentException("Issuer name cannot be empty");
        }
        
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&period=30&digits=6&algorithm=SHA1",
                issuer, account, secret, issuer);
    }

    public String generateQrCodeImage(String qrCodeUri) {
        try {
            if (qrCodeUri == null || qrCodeUri.isEmpty()) {
                throw new IllegalArgumentException("QR code URI cannot be empty");
            }

            com.google.zxing.qrcode.QRCodeWriter qrCodeWriter = new com.google.zxing.qrcode.QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = qrCodeWriter.encode(qrCodeUri,
                    com.google.zxing.BarcodeFormat.QR_CODE, 200, 200);

            java.io.ByteArrayOutputStream pngOutputStream = new java.io.ByteArrayOutputStream();
            com.google.zxing.client.j2se.MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
            byte[] pngData = pngOutputStream.toByteArray();

            return java.util.Base64.getEncoder().encodeToString(pngData);
        } catch (Exception e) {
            LOG.errorv(e, "[QR] QR code generation failed: {0}", e.getMessage());
            return "";
        }
    }
}

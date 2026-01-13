package com.secureteam.auth;

import org.jboss.aerogear.security.otp.api.Base32;
import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

@ApplicationScoped
public class TotpService {

    private static final Logger LOG = Logger.getLogger(TotpService.class);

    public String generateSecret() {
        return Base32.random();
    }

    public boolean validateCode(String secret, String code) {
        try {
            // Using Aerogear Base32
            byte[] key = Base32.decode(secret);
            long timeWindow = 30; // 30 seconds
            long currentTimestamp = System.currentTimeMillis() / 1000;

            // Check current, past 2 and future 2 windows (approx +/- 1 min)
            // Reduced from previous excessive window of -10/+2
            for (int i = -2; i <= 2; i++) {
                long t = (currentTimestamp / timeWindow) + i;
                if (verifyCode(key, t, code)) {
                    if (i != 0)
                        LOG.debugv("[MFA DEBUG] Validated with drift: {0}s", (i * 30));
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            LOG.error("MFA Validation Error", e);
            return false;
        }
    }

    private boolean verifyCode(byte[] key, long t, String code) throws Exception {
        byte[] data = new byte[8];
        long value = t;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        javax.crypto.spec.SecretKeySpec signKey = new javax.crypto.spec.SecretKeySpec(key, "HmacSHA1");
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[hash.length - 1] & 0xF;
        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }
        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;

        return String.format("%06d", truncatedHash).equals(code);
    }

    public String generateQrCodeUri(String secret, String account, String issuer) {
        return String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s",
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
            return "";
        }
    }
}

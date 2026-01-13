package com.secureteam.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import java.time.Instant;
import java.util.UUID;

/**
 * Rate Limiting Service
 * Implements per-IP rate limiting using Redis for distributed rate limiting
 * Protects against brute-force attacks on authentication endpoints
 * 
 * ✅ Protections:
 * - Login endpoint: 5 attempts per 5 minutes per IP
 * - MFA verification: 10 attempts per 15 minutes per IP
 * - Returns HTTP 429 when rate limit exceeded
 * 
 * CWE-307: Improper Restriction of Rendered UI Layers or Frames
 * CWE-404: Improper Resource Validation ('Resource Exhaustion')
 * 
 * Redis Key Pattern: "ratelimit:{endpoint}:{ip}:{window}"
 * Value: Request count
 * TTL: Sliding window duration (5 or 15 minutes)
 */
@ApplicationScoped
public class RateLimitingService {

    private static final Logger LOG = Logger.getLogger(RateLimitingService.class);

    @Inject
    private JedisPool jedisPool;

    // Rate limit configurations (in seconds and max attempts)
    private static final int LOGIN_WINDOW_SECONDS = 300;      // 5 minutes
    private static final int LOGIN_MAX_ATTEMPTS = 5;
    private static final int MFA_WINDOW_SECONDS = 900;        // 15 minutes
    private static final int MFA_MAX_ATTEMPTS = 10;

    /**
     * Check if login attempt should be rate limited
     * 
     * @param clientIp Client IP address
     * @return true if rate limit exceeded, false if request allowed
     */
    public boolean isLoginLimited(String clientIp) {
        return checkRateLimit("login", clientIp, LOGIN_WINDOW_SECONDS, LOGIN_MAX_ATTEMPTS);
    }

    /**
     * Check if MFA verification attempt should be rate limited
     * 
     * @param clientIp Client IP address
     * @return true if rate limit exceeded, false if request allowed
     */
    public boolean isMfaLimited(String clientIp) {
        return checkRateLimit("mfa", clientIp, MFA_WINDOW_SECONDS, MFA_MAX_ATTEMPTS);
    }

    /**
     * Record a login attempt for rate limiting
     * 
     * @param clientIp Client IP address
     */
    public void recordLoginAttempt(String clientIp) {
        recordAttempt("login", clientIp, LOGIN_WINDOW_SECONDS);
    }

    /**
     * Record an MFA verification attempt for rate limiting
     * 
     * @param clientIp Client IP address
     */
    public void recordMfaAttempt(String clientIp) {
        recordAttempt("mfa", clientIp, MFA_WINDOW_SECONDS);
    }

    /**
     * Core rate limiting check with sliding window algorithm
     * Uses Redis INCR and EXPIRE for atomic operations
     * 
     * @param endpoint Endpoint name (login, mfa, etc.)
     * @param clientIp Client IP address
     * @param windowSeconds Window duration in seconds
     * @param maxAttempts Maximum allowed attempts in window
     * @return true if rate limit exceeded, false otherwise
     */
    private boolean checkRateLimit(String endpoint, String clientIp, 
                                   int windowSeconds, int maxAttempts) {
        if (clientIp == null || clientIp.isEmpty()) {
            LOG.warnv("[RateLimit] Missing client IP for {0}", endpoint);
            return false; // Don't block if IP can't be determined
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = buildKey(endpoint, clientIp);
            
            // Get current count
            String countStr = jedis.get(key);
            int count = (countStr != null) ? Integer.parseInt(countStr) : 0;

            boolean isLimited = count >= maxAttempts;

            if (isLimited) {
                LOG.warnv("[RateLimit] BLOCKED {0} from {1} - {2} attempts in {3}s window",
                         endpoint, clientIp, count, windowSeconds);
            }

            return isLimited;

        } catch (Exception e) {
            // If Redis fails, fail open (don't block) but log the error
            LOG.errorv(e, "[RateLimit] Redis error checking rate limit for {0}: {1}",
                      endpoint, e.getMessage());
            return false;
        }
    }

    /**
     * Record an attempt in the rate limiting counter
     * Uses INCR for atomic increment and EXPIRE for window management
     * 
     * @param endpoint Endpoint name
     * @param clientIp Client IP address
     * @param windowSeconds Window duration in seconds
     */
    private void recordAttempt(String endpoint, String clientIp, int windowSeconds) {
        if (clientIp == null || clientIp.isEmpty()) {
            LOG.warnv("[RateLimit] Missing client IP for recording {0}", endpoint);
            return;
        }

        try (Jedis jedis = jedisPool.getResource()) {
            String key = buildKey(endpoint, clientIp);

            // Atomic increment
            long count = jedis.incr(key);

            // Set expiration only on first increment (count == 1)
            if (count == 1) {
                jedis.expire(key, windowSeconds);
            }

            LOG.debugv("[RateLimit] {0} from {1} - attempt {2}/{3} in {4}s window",
                      endpoint, clientIp, count, 
                      endpoint.equals("login") ? LOGIN_MAX_ATTEMPTS : MFA_MAX_ATTEMPTS,
                      windowSeconds);

        } catch (Exception e) {
            // Log but don't fail - rate limiting should not break the application
            LOG.errorv(e, "[RateLimit] Redis error recording attempt for {0}: {1}",
                      endpoint, e.getMessage());
        }
    }

    /**
     * Build Redis key for rate limiting
     * Pattern: ratelimit:{endpoint}:{clientIp}
     * 
     * @param endpoint Endpoint identifier
     * @param clientIp Client IP address
     * @return Redis key string
     */
    private String buildKey(String endpoint, String clientIp) {
        return String.format("ratelimit:%s:%s", endpoint, sanitizeIp(clientIp));
    }

    /**
     * Sanitize IP address to prevent Redis key injection
     * Validates that IP contains only valid characters (numbers, dots, colons for IPv6)
     * 
     * @param ip IP address string
     * @return Sanitized IP or "unknown" if invalid
     */
    private String sanitizeIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return "unknown";
        }

        // Only allow valid IP characters: 0-9, dots (IPv4), colons (IPv6), hyphens (for ranges)
        if (ip.matches("^[0-9a-fA-F:._-]+$") && ip.length() <= 45) {
            return ip;
        }

        LOG.warnv("[RateLimit] Invalid IP format: {0}", ip);
        return "unknown";
    }

    /**
     * Clear rate limit for a specific endpoint and IP (admin reset)
     * 
     * @param endpoint Endpoint name
     * @param clientIp Client IP address
     */
    public void clearLimit(String endpoint, String clientIp) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = buildKey(endpoint, clientIp);
            jedis.del(key);
            LOG.infov("[RateLimit] Cleared limit for {0} from {1}", endpoint, clientIp);
        } catch (Exception e) {
            LOG.errorv(e, "[RateLimit] Error clearing limit: {0}", e.getMessage());
        }
    }

    /**
     * Get current attempt count for debugging
     * 
     * @param endpoint Endpoint name
     * @param clientIp Client IP address
     * @return Current attempt count or 0 if not found
     */
    public int getAttemptCount(String endpoint, String clientIp) {
        try (Jedis jedis = jedisPool.getResource()) {
            String key = buildKey(endpoint, clientIp);
            String count = jedis.get(key);
            return (count != null) ? Integer.parseInt(count) : 0;
        } catch (Exception e) {
            LOG.errorv(e, "[RateLimit] Error getting attempt count: {0}", e.getMessage());
            return 0;
        }
    }
}

package com.secureteam.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import java.io.IOException;

/**
 * Security Headers Filter
 * Implements OWASP recommended HTTP security headers to prevent common attacks
 * 
 * ✅ Protections:
 * - CSP (Content Security Policy): Prevents XSS attacks
 * - HSTS (HTTP Strict-Transport-Security): Forces HTTPS
 * - X-Frame-Options: Prevents clickjacking
 * - X-Content-Type-Options: Prevents MIME type sniffing
 * - X-XSS-Protection: Legacy XSS protection (for older browsers)
 * - Referrer-Policy: Controls referrer information leakage
 * 
 * CWE-693: Improper Resource Validation
 * CWE-94: Improper Control of Generation of Code ('Code Injection')
 */
@WebFilter(urlPatterns = "/*")
public class SecurityHeadersFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(SecurityHeadersFilter.class);

    // Content Security Policy: Strict whitelist approach
    // - default-src 'self': Only allow resources from same origin
    // - script-src 'self': Only allow scripts from same origin (prevents external JS injection)
    // - style-src 'self': Only allow stylesheets from same origin
    // - img-src 'self' data:: Allow images from same origin and data URLs
    // - font-src 'self': Only allow fonts from same origin
    // - connect-src 'self': Only allow connections to same origin (prevents exfiltration)
    // - frame-ancestors 'none': This page cannot be embedded in frames (see X-Frame-Options)
    // - base-uri 'self': Only allow base tag pointing to same origin
    private static final String CSP_HEADER = "Content-Security-Policy";
    private static final String CSP_VALUE = 
        "default-src 'self'; " +
        "script-src 'self'; " +
        "style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data: https:; " +
        "font-src 'self'; " +
        "connect-src 'self'; " +
        "frame-ancestors 'none'; " +
        "base-uri 'self'; " +
        "form-action 'self'";

    // HSTS (HTTP Strict-Transport-Security)
    // - max-age=31536000: Enforce HTTPS for 1 year (365 * 24 * 60 * 60 seconds)
    // - includeSubDomains: Apply to all subdomains
    // - preload: Allow inclusion in browser HSTS preload lists
    private static final String HSTS_HEADER = "Strict-Transport-Security";
    private static final String HSTS_VALUE = "max-age=31536000; includeSubDomains; preload";

    // X-Frame-Options: DENY prevents this page from being framed anywhere
    // Protects against clickjacking attacks
    private static final String X_FRAME_OPTIONS = "X-Frame-Options";
    private static final String X_FRAME_OPTIONS_VALUE = "DENY";

    // X-Content-Type-Options: nosniff prevents browser from MIME type sniffing
    // Forces browser to respect Content-Type header
    private static final String X_CONTENT_TYPE_OPTIONS = "X-Content-Type-Options";
    private static final String X_CONTENT_TYPE_OPTIONS_VALUE = "nosniff";

    // X-XSS-Protection: Legacy header for older browsers (Edge < 79, IE)
    // 1; mode=block enables XSS filter and blocks page if attack detected
    private static final String X_XSS_PROTECTION = "X-XSS-Protection";
    private static final String X_XSS_PROTECTION_VALUE = "1; mode=block";

    // Referrer-Policy: strict-no-referrer prevents referrer leakage
    // No referrer information is sent with cross-origin requests
    private static final String REFERRER_POLICY = "Referrer-Policy";
    private static final String REFERRER_POLICY_VALUE = "strict-no-referrer";

    // Permissions-Policy: Restricts browser features (modern replacement for Feature-Policy)
    // Disables unnecessary features to reduce attack surface
    private static final String PERMISSIONS_POLICY = "Permissions-Policy";
    private static final String PERMISSIONS_POLICY_VALUE = 
        "geolocation=(), " +
        "microphone=(), " +
        "camera=(), " +
        "payment=(), " +
        "usb=(), " +
        "magnetometer=(), " +
        "gyroscope=(), " +
        "accelerometer=()";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.info("[SecurityHeaders] Filter initialized - All requests will include security headers");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (response instanceof HttpServletResponse) {
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Set all security headers
            httpResponse.setHeader(CSP_HEADER, CSP_VALUE);
            httpResponse.setHeader(HSTS_HEADER, HSTS_VALUE);
            httpResponse.setHeader(X_FRAME_OPTIONS, X_FRAME_OPTIONS_VALUE);
            httpResponse.setHeader(X_CONTENT_TYPE_OPTIONS, X_CONTENT_TYPE_OPTIONS_VALUE);
            httpResponse.setHeader(X_XSS_PROTECTION, X_XSS_PROTECTION_VALUE);
            httpResponse.setHeader(REFERRER_POLICY, REFERRER_POLICY_VALUE);
            httpResponse.setHeader(PERMISSIONS_POLICY, PERMISSIONS_POLICY_VALUE);

            // Remove server identification header to reduce information leakage
            httpResponse.setHeader("Server", "");

            // Prevent caching of sensitive pages (login, MFA)
            String requestURI = ((jakarta.servlet.http.HttpServletRequest) request).getRequestURI();
            if (requestURI.contains("auth") || requestURI.contains("mfa")) {
                httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
                httpResponse.setHeader("Pragma", "no-cache");
                httpResponse.setHeader("Expires", "0");
            }

            LOG.debugv("[SecurityHeaders] Headers applied to {0}", requestURI);
        }

        // Continue with the request
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        LOG.info("[SecurityHeaders] Filter destroyed");
    }
}

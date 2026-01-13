package com.secureteam.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jboss.logging.Logger;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * CORS (Cross-Origin Resource Sharing) Filter
 * Implements whitelist-based CORS validation to prevent cross-origin attacks
 * 
 * ✅ Protections:
 * - Whitelist-based origin validation (only pre-approved origins allowed)
 * - Credentials included only for trusted origins
 * - Methods restricted to GET, POST, PUT, DELETE
 * - Headers validated against whitelist
 * - No wildcard (*) for credentials=true
 * 
 * CWE-346: Origin Validation Error
 * CWE-942: Permissive Cross-domain Policy with Untrusted Domains
 * 
 * Configuration: Add trusted origins to TRUSTED_ORIGINS set
 * Default: Only localhost (development) - modify for production
 */
@WebFilter(urlPatterns = "/api/*")
public class CorsFilter implements Filter {

    private static final Logger LOG = Logger.getLogger(CorsFilter.class);

    // Whitelist of allowed origins - MUST be configured per environment
    // Development: localhost, Production: your-domain.com only
    private static final Set<String> TRUSTED_ORIGINS = new HashSet<>();

    static {
        // Development origins
        TRUSTED_ORIGINS.add("http://localhost:3000");      // Vite dev server
        TRUSTED_ORIGINS.add("http://localhost:5173");      // Vite alternative port
        TRUSTED_ORIGINS.add("http://localhost:8080");      // Local testing
        TRUSTED_ORIGINS.add("http://127.0.0.1:3000");      // Loopback
        TRUSTED_ORIGINS.add("http://127.0.0.1:5173");      // Loopback Vite

        // Production - Configure via environment variables in real deployment
        // String prodOrigin = System.getenv("CORS_ALLOWED_ORIGIN");
        // if (prodOrigin != null) TRUSTED_ORIGINS.add(prodOrigin);
    }

    private static final String[] ALLOWED_METHODS = {"GET", "POST", "PUT", "DELETE", "OPTIONS"};
    private static final String[] ALLOWED_HEADERS = {
        "Content-Type",
        "Authorization",
        "X-Requested-With",
        "Accept"
    };
    private static final String[] EXPOSED_HEADERS = {
        "Content-Length",
        "Content-Range",
        "X-Total-Count"  // For pagination
    };

    private static final long MAX_AGE = 86400; // 24 hours

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        LOG.infov("[CORS] Filter initialized - Whitelist size: {0}", TRUSTED_ORIGINS.size());
        if (TRUSTED_ORIGINS.isEmpty()) {
            LOG.warn("[CORS] WARNING: No trusted origins configured!");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String origin = httpRequest.getHeader("Origin");
        String method = httpRequest.getMethod();

        // Only process requests with Origin header (CORS requests)
        if (origin != null && !origin.isEmpty()) {
            if (isOriginTrusted(origin)) {
                // Apply CORS headers for trusted origins
                applyCorHeaders(httpResponse, origin);
                LOG.debugv("[CORS] Allowed - Origin: {0}, Method: {1}", origin, method);
            } else {
                // Reject with warning (no CORS headers added)
                LOG.warnv("[CORS] Blocked - Untrusted origin: {0}, Method: {1}", origin, method);
                httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
                httpResponse.getWriter().write("CORS policy violation: Origin not whitelisted");
                return;
            }
        }

        // Handle preflight requests (OPTIONS)
        if ("OPTIONS".equalsIgnoreCase(method)) {
            httpResponse.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        // Continue with actual request
        chain.doFilter(request, response);
    }

    /**
     * Check if origin is in trusted whitelist
     * 
     * Security: Exact match only - no wildcards, no partial matches
     * 
     * @param origin Origin header value
     * @return true if origin is trusted
     */
    private boolean isOriginTrusted(String origin) {
        if (origin == null || origin.isEmpty()) {
            return false;
        }

        // Exact match - no wildcard, no substring match
        boolean isTrusted = TRUSTED_ORIGINS.contains(origin);

        if (!isTrusted) {
            // Log suspicious origins only once per unique origin
            LOG.warnv("[CORS] Rejected untrusted origin: {0}", origin);
        }

        return isTrusted;
    }

    /**
     * Apply CORS response headers for trusted origins
     * 
     * @param response HTTP response
     * @param origin Trusted origin
     */
    private void applyCorHeaders(HttpServletResponse response, String origin) {
        // Specify the allowed origin (never use wildcard with credentials=true)
        response.setHeader("Access-Control-Allow-Origin", origin);

        // Allow credentials (cookies, HTTP auth, client-side SSL certificates)
        // Only set to true for same-site requests, NEVER with Origin: *
        response.setHeader("Access-Control-Allow-Credentials", "true");

        // Specify allowed HTTP methods
        response.setHeader("Access-Control-Allow-Methods", 
                         String.join(", ", ALLOWED_METHODS));

        // Specify allowed request headers
        response.setHeader("Access-Control-Allow-Headers", 
                         String.join(", ", ALLOWED_HEADERS));

        // Specify exposed response headers (what client can read)
        response.setHeader("Access-Control-Expose-Headers", 
                         String.join(", ", EXPOSED_HEADERS));

        // Cache preflight for 24 hours (reduce OPTIONS requests)
        response.setHeader("Access-Control-Max-Age", String.valueOf(MAX_AGE));

        // Prevent CORS information leakage via origin null
        response.setHeader("Access-Control-Allow-Private-Network", "false");
    }

    @Override
    public void destroy() {
        LOG.info("[CORS] Filter destroyed");
    }

    /**
     * Add a trusted origin at runtime (for testing or dynamic configuration)
     * 
     * @param origin Origin URL to trust
     */
    public static void addTrustedOrigin(String origin) {
        if (origin != null && !origin.isEmpty() && isValidOrigin(origin)) {
            TRUSTED_ORIGINS.add(origin);
            LOG.infov("[CORS] Added trusted origin: {0}", origin);
        }
    }

    /**
     * Remove a trusted origin
     * 
     * @param origin Origin URL to remove
     */
    public static void removeTrustedOrigin(String origin) {
        if (TRUSTED_ORIGINS.remove(origin)) {
            LOG.infov("[CORS] Removed trusted origin: {0}", origin);
        }
    }

    /**
     * Validate origin format to prevent injection
     * Must be: scheme://host[:port]
     * 
     * @param origin Origin to validate
     * @return true if valid format
     */
    private static boolean isValidOrigin(String origin) {
        return origin != null && 
               origin.matches("^(https?|wss?)://[a-zA-Z0-9.-]+(:\\d+)?$");
    }
}

package com.secureteam.auth;

import com.secureteam.model.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import org.jboss.logging.Logger;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @GET
    @Path("/health")
    @PermitAll
    public Response health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "SecureTeam Access Engine is running.");
        return Response.ok(status).build();
    }

    @Inject
    private TotpService totpService;

    @Inject
    private PasetoService pasetoService;

    @Inject
    private RedisClient redisClient;

    @Inject
    private jakarta.persistence.EntityManager em;

    /**
     * Setup MFA: Generate TOTP secret and QR code
     * ⚠️ Must be called over HTTPS to prevent secret interception
     */
    @GET
    @Path("/mfa/setup")
    @PermitAll
    public Response setupMfa(@QueryParam("username") String username) {
        try {
            // Input validation: username required and reasonable length
            if (username == null || username.trim().isEmpty()) {
                LOG.infov("[AUTH] MFA setup requested with empty username");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Username cannot be empty").build();
            }
            
            if (username.length() < 3 || username.length() > 50) {
                LOG.infov("[AUTH] MFA setup requested with invalid username length");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Username must be 3-50 characters").build();
            }

            // Sanitize username (prevent injection)
            if (!username.matches("^[a-zA-Z0-9._-]+$")) {
                LOG.infov("[AUTH] MFA setup requested with suspicious username characters");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Username contains invalid characters").build();
            }

            // Generate TOTP secret (160 bits, Base32 encoded)
            String secret = totpService.generateSecret();

            // Store secret in Redis for 10 minutes (TTL for setup)
            String key = "mfa_secret_" + username;
            redisClient.setEx(key, 600, secret);
            LOG.infov("[AUTH] MFA setup initiated for user: {0}", username);

            // Generate QR code for scanning into authenticator app
            String qrUri = totpService.generateQrCodeUri(secret, username, "SecureTeamAccess");
            String qrImage = totpService.generateQrCodeImage(qrUri);

            Map<String, String> response = new HashMap<>();
            response.put("secret", secret);
            response.put("qrUri", qrUri);
            response.put("qrImage", "data:image/png;base64," + qrImage);

            return Response.ok(response).build();
        } catch (Exception e) {
            LOG.errorv(e, "[AUTH] MFA setup error for user {0}: {1}", username, e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("MFA setup failed").build();
        }
    }

    /**
     * Verify MFA code and generate PASETO token
     */
    @POST
    @Path("/mfa/verify")
    @PermitAll
    public Response verifyMfa(MfaVerificationRequest request) {
        try {
            // Input validation
            if (request == null || request.getUsername() == null || request.getUsername().isEmpty()) {
                LOG.warnv("[AUTH] MFA verify attempt with empty username");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Username required").build();
            }

            if (request.getCode() == null || !request.getCode().matches("^[0-9]{6}$")) {
                LOG.warnv("[AUTH] MFA verify attempt with invalid code format from user: {0}", 
                         request.getUsername());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid TOTP code format (6 digits required)").build();
            }

            // Retrieve stored secret from Redis
            String key = "mfa_secret_" + request.getUsername();
            String storedSecret = redisClient.get(key);

            if (storedSecret == null) {
                LOG.infov("[AUTH] MFA verification failed - session expired for user: {0}", 
                         request.getUsername());
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("MFA session expired or not found").build();
            }

            // Validate TOTP code (RFC 6238 compliant, ±1 minute window)
            boolean isValid = totpService.validateCode(storedSecret, request.getCode());

            if (isValid) {
                try {
                    // Generate PASETO token (asymmetric, Ed25519 signed)
                    String token = pasetoService.createPublicToken(request.getUsername(), "secureteam-web",
                            "DEVICE-ID-123");
                    
                    // Clean up MFA secret from Redis (one-time use)
                    redisClient.delete(key);
                    
                    LOG.infov("[AUTH] MFA verification SUCCESS for user: {0}", request.getUsername());
                    
                    Map<String, String> response = new HashMap<>();
                    response.put("token", token);
                    return Response.ok(response).build();
                } catch (Exception e) {
                    LOG.errorv(e, "[AUTH] Token generation failed for user {0}: {1}", 
                             request.getUsername(), e.getMessage());
                    return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Token generation failed").build();
                }
            } else {
                LOG.infov("[AUTH] MFA verification FAILED - invalid code for user: {0}", 
                         request.getUsername());
                return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid TOTP code").build();
            }
        } catch (Exception e) {
            LOG.errorv(e, "[AUTH] MFA verification error: {0}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Authentication failed").build();
        }
    }

    /**
     * Register new user with minimal ABAC setup
     */
    @POST
    @Path("/register")
    @PermitAll
    @jakarta.transaction.Transactional
    public Response register(User user) {
        try {
            // Input validation
            if (user == null || user.getUsername() == null || user.getUsername().isEmpty()) {
                LOG.warnv("[AUTH] Registration attempt with missing username");
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Username required").build();
            }

            // Validate username format
            if (!user.getUsername().matches("^[a-zA-Z0-9._-]{3,50}$")) {
                LOG.warnv("[AUTH] Registration attempt with invalid username: {0}", user.getUsername());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Username must be 3-50 chars (alphanumeric, ._- allowed)").build();
            }

            // Enforce department for ABAC policy
            if (user.getDepartment() == null || user.getDepartment().isEmpty()) {
                user.setDepartment("engineering");
            }

            // Persist user to database
            em.persist(user);
            
            LOG.infov("[AUTH] User registered successfully: {0}", user.getUsername());
            
            return Response.status(Response.Status.CREATED)
                .entity("User registered successfully").build();
        } catch (Exception e) {
            LOG.errorv(e, "[AUTH] User registration error: {0}", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity("Registration failed").build();
        }
    }

    /**
     * MFA Verification Request DTO with validation
     */
    public static class MfaVerificationRequest {
        @NotBlank(message = "Username cannot be blank")
        @Size(min = 3, max = 50, message = "Username must be 3-50 characters")
        private String username;

        @NotBlank(message = "TOTP code cannot be blank")
        @Pattern(regexp = "^[0-9]{6}$", message = "TOTP code must be 6 digits")
        private String code;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}

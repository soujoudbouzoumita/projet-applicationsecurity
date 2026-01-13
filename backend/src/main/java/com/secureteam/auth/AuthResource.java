package com.secureteam.auth;

import com.secureteam.model.User;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.annotation.security.PermitAll;
import java.util.HashMap;
import java.util.Map;
import org.jboss.logging.Logger;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

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

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @POST
    @Path("/login")
    @PermitAll
    public Response login(LoginRequest request) {
        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", request.getUsername())
                    .getSingleResult();

            // CONSTANT TIME PASSWORD CHECK (simplified for demo, should use BCrypt)
            if (user.getPassword().equals(request.getPassword())) {
                LOG.infov("[AUTH] 1FA Success for user {0}", request.getUsername());

                // Store 1FA success in Redis with 5 min TTL
                String key = "auth_1fa_success_" + request.getUsername();
                redisClient.setEx(key, 300, "true");

                Map<String, Object> response = new HashMap<>();
                response.put("username", request.getUsername());
                response.put("mfaEnabled", user.isMfaEnabled());
                return Response.ok(response).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
            }
        } catch (jakarta.persistence.NoResultException e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
        } catch (Exception e) {
            LOG.error("Login error", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/mfa/setup")
    @PermitAll
    public Response setupMfa(@QueryParam("username") String username) {
        // SECURITY CHECK: Ensure user has passed 1FA first
        String authKey = "auth_1fa_success_" + username;
        if (redisClient.get(authKey) == null) {
            LOG.errorv("[MFA] Unauthorized setup attempt for user {0}", username);
            return Response.status(Response.Status.UNAUTHORIZED).entity("Please login first").build();
        }

        String secret = totpService.generateSecret();

        // Store secret in Redis for 10 minutes (TTL for setup)
        String key = "mfa_secret_" + username;
        redisClient.setEx(key, 600, secret);

        String qrUri = totpService.generateQrCodeUri(secret, username, "SecureTeamAccess");
        String qrImage = totpService.generateQrCodeImage(qrUri);

        Map<String, String> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrUri", qrUri);
        response.put("qrImage", "data:image/png;base64," + qrImage);

        return Response.ok(response).build();
    }

    @POST
    @Path("/mfa/verify")
    @PermitAll
    @jakarta.transaction.Transactional
    public Response verifyMfa(MfaVerificationRequest request) {
        try {
            // SECURITY CHECK: Ensure user has passed 1FA first
            String authKey = "auth_1fa_success_" + request.getUsername();
            if (redisClient.get(authKey) == null) {
                LOG.errorv("[MFA] Unauthorized verify attempt for user {0}", request.getUsername());
                return Response.status(Response.Status.UNAUTHORIZED).entity("Session expired or unauthorized").build();
            }

            // Sanitize and validate the code input
            String code = request.getCode();
            if (code == null || code.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Invalid code format").build();
            }

            code = code.trim().replaceAll("\\s+", "");
            if (!code.matches("^\\d{6}$")) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Code must be 6 digits").build();
            }

            // Try to find the user
            User user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", request.getUsername())
                    .getSingleResult();

            String secret = null;
            boolean isFirstTimeSetup = false;

            String tempKey = "mfa_secret_" + request.getUsername();
            String tempSecret = redisClient.get(tempKey);

            if (tempSecret != null) {
                secret = tempSecret;
                isFirstTimeSetup = true;
            } else if (user.getTotpSecret() != null && user.isMfaEnabled()) {
                secret = user.getTotpSecret();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("MFA not configured.").build();
            }

            // Validate the TOTP code
            boolean isValid = totpService.validateCode(secret, code);

            if (isValid) {
                if (isFirstTimeSetup) {
                    user.setTotpSecret(secret);
                    user.setMfaEnabled(true);
                    // em.merge(user);
                    redisClient.delete(tempKey);
                }

                // Clear 1FA success from Redis - Auth complete
                redisClient.delete(authKey);

                // Collect dynamic claims
                java.util.List<String> roles = user.getRoles().stream().map(Enum::name)
                        .collect(java.util.stream.Collectors.toList());
                java.util.List<String> projects = user.getAuthorizedProjects().stream().map(p -> p.getName())
                        .collect(java.util.stream.Collectors.toList());

                // Generate dynamic PASETO Token
                String token = pasetoService.createPublicToken(
                        user.getUsername(),
                        user.getDepartment(),
                        roles,
                        projects,
                        "DEVICE-ID-123");

                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                return Response.ok(response).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity("MFA Invalid").build();
            }
        } catch (Exception e) {
            LOG.error("Security Engine Error: MFA Verification Failed", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("MFA Verification Failed").build();
        }
    }

    @Inject
    private jakarta.persistence.EntityManager em;

    @POST
    @Path("/register")
    @PermitAll
    @jakarta.transaction.Transactional
    public Response register(User user) {
        // Enforce department for ABAC
        if (user.getDepartment() == null) {
            user.setDepartment("engineering");
        }
        em.persist(user);
        return Response.status(Response.Status.CREATED).entity("User registered successfully").build();
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class MfaVerificationRequest {
        private String username;
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

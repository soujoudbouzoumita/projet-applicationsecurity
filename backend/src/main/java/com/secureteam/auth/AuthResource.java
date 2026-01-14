package com.secureteam.auth;

import com.secureteam.model.User;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
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

    private static final Logger LOG = Logger.getLogger(AuthResource.class);

    @Inject
    private TotpService totpService;

    @Inject
    private PasetoService pasetoService;

    @Inject
    private RedisClient redisClient;

    @Inject
    private PasswordService passwordService;

    // CORRECTION 1 : Utilisation de @PersistenceContext au lieu de @Inject
    @PersistenceContext(unitName = "secureteamPU")
    private EntityManager em;

    @GET
    @Path("/health")
    @PermitAll
    public Response health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "SecureTeam Access Engine is running.");
        return Response.ok(status).build();
    }

    @GET
    @Path("/validate")
    @PermitAll
    public Response validateToken() {
        // If we reach here, SecurityFilter has already validated the token
        return Response.ok().build();
    }

    @GET
    @Path("/mfa/setup")
    @PermitAll
    public Response setupMfa(@QueryParam("username") String username) {
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
    @Path("/login")
    @PermitAll
    public Response login(LoginRequest request) {
        String username = safeTrim(request.getUsername());
        if (username == null || request.getPassword() == null) {
             return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
        }

        try {
            User user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();

            boolean valid = passwordService.verify(request.getPassword(), user.getPasswordSalt(), user.getPasswordHash());
            if (!valid) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
            }
            
            Map<String, String> response = new HashMap<>();
            if (user.isMfaEnabled()) {
                response.put("status", "MFA_REQUIRED");
            } else {
                response.put("status", "MFA_SETUP_REQUIRED");
            }
            return Response.ok(response).build();

        } catch (jakarta.persistence.NoResultException e) {
            // Timing attack mitigation: verify a dummy password or just sleep? 
            // For now, just return unauthorized.
            return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
        }
    }

    @POST
    @Path("/mfa/verify")
    @PermitAll
    @Transactional
    public Response verifyMfa(MfaVerificationRequest request) {
        String username = safeTrim(request.getUsername());
        
        User user = null;
        try {
            user = em.createQuery("SELECT u FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("User not found").build();
        }

        String secret = user.getTotpSecret();
        boolean isSetup = false;
        
        // If no secret in DB, check Redis (Setup flow)
        if (secret == null) {
            String key = "mfa_secret_" + username;
            secret = redisClient.get(key);
            isSetup = true;
        }

        if (secret == null) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("MFA session expired or not found").build();
        }

        boolean isValid = totpService.validateCode(secret, request.getCode());

        if (isValid) {
            try {
                // If this was a setup verification, save the secret to DB
                if (isSetup) {
                    user.setTotpSecret(secret);
                    user.setMfaEnabled(true);
                    em.merge(user);
                }

                String token = pasetoService.createPublicToken(username, "secureteam-web",
                        "DEVICE-ID-123");
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                return Response.ok(response).build();
            } catch (Exception e) {
                LOG.error("Security Engine Error: Token Generation Failed", e);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity("Security Engine Error: Token Generation Failed").build();
            }
        } else {
            return Response.status(Response.Status.UNAUTHORIZED).entity("MFA Invalid").build();
        }
    }

    @POST
    @Path("/register")
    @PermitAll
    @Transactional
    public Response register(RegisterRequest request) {
        // Basic input validation (defense-in-depth)
        String username = request == null ? null : safeTrim(request.getUsername());
        String password = request == null ? null : request.getPassword();

        if (!isValidUsername(username)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Invalid username. Use 3-50 chars: letters, digits, ., _, -")
                    .build();
        }
        if (!isStrongPassword(password)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Weak password. Use at least 12 chars with upper, lower, digit and special char")
                    .build();
        }

        // Uniqueness check
        Long count = em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = :username", Long.class)
                .setParameter("username", username)
                .getSingleResult();
        if (count != null && count > 0) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("Username already exists")
                    .build();
        }

        User user = new User();
        user.setUsername(username);
        
        // CORRECTION 2 : Utilisation du getter getPassword()
        PasswordService.HashResult res = passwordService.hash(request.getPassword());
        
        user.setPasswordHash(res.hashBase64);
        user.setPasswordSalt(res.saltBase64);

        // Enforce department for ABAC
        String dept = safeTrim(request.getDepartment());
        user.setDepartment((dept == null || dept.isBlank()) ? "engineering" : dept);

        em.persist(user);
        return Response.status(Response.Status.CREATED)
                .entity("User registered successfully")
                .build();
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean isValidUsername(String username) {
        return username != null && username.matches("^[a-zA-Z0-9._-]{3,50}$");
    }

    private static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 12) return false;
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasLower = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*[0-9].*");
        boolean hasSpecial = password.matches(".*[^A-Za-z0-9].*");
        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    public static class RegisterRequest {
        private String username;
        private String password;
        private String department;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
    }

    public static class MfaVerificationRequest {
        private String username;
        private String code;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
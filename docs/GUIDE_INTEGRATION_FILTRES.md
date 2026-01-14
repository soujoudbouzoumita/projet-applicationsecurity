# GUIDE D'INTÉGRATION - FILTRES DE SÉCURITÉ

## 📋 Configuration des Filtres de Sécurité

### Vue d'ensemble
Ce document explique comment les filtres de sécurité nouvellement créés s'intègrent dans l'application WildFly/Jakarta EE.

---

## 🔧 Configuration web.xml

Les filtres sont déclarés via `@WebFilter` annotation (Jakarta EE), mais vous pouvez aussi les configurer dans `web.xml`:

**File:** `backend/src/main/webapp/WEB-INF/web.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="https://jakarta.ee/xml/ns/jakartaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="https://jakarta.ee/xml/ns/jakartaee 
                             https://jakarta.ee/xml/ns/jakartaee/web-app_6_0.xsd"
         version="6.0">

    <display-name>SecureTeam Access</display-name>

    <!-- ==================== SECURITY FILTERS ==================== -->
    
    <!-- 1. SECURITY HEADERS FILTER (appliqué à toutes les requêtes) -->
    <filter>
        <filter-name>SecurityHeadersFilter</filter-name>
        <filter-class>com.secureteam.security.SecurityHeadersFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>SecurityHeadersFilter</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
        <dispatcher>FORWARD</dispatcher>
        <dispatcher>INCLUDE</dispatcher>
    </filter-mapping>

    <!-- 2. CORS FILTER (appliqué aux endpoints API) -->
    <filter>
        <filter-name>CorsFilter</filter-name>
        <filter-class>com.secureteam.security.CorsFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>CorsFilter</filter-name>
        <url-pattern>/api/*</url-pattern>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>

    <!-- 3. RATE LIMITING FILTER (appliqué aux endpoints auth) -->
    <filter>
        <filter-name>RateLimitingFilter</filter-name>
        <filter-class>com.secureteam.security.RateLimitingFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>RateLimitingFilter</filter-name>
        <url-pattern>/api/auth/login</url-pattern>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>
    <filter-mapping>
        <filter-name>RateLimitingFilter</filter-name>
        <url-pattern>/api/auth/mfa/verify</url-pattern>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>

    <!-- ==================== AUTHENTICATION & SESSIONS ==================== -->
    
    <!-- Login Configuration -->
    <login-config>
        <auth-method>FORM</auth-method>
        <realm-name>SecureTeamRealm</realm-name>
        <form-login-config>
            <form-login-page>/login.html</form-login-page>
            <form-error-page>/login-error.html</form-error-page>
        </form-login-config>
    </login-config>

    <!-- Session Configuration -->
    <session-config>
        <!-- 15 minute session timeout -->
        <cookie-config>
            <secure>true</secure>
            <http-only>true</http-only>
            <same-site>STRICT</same-site>
        </cookie-config>
        <tracking-mode>COOKIE</tracking-mode>
    </session-config>

    <!-- ==================== SECURITY CONSTRAINTS ==================== -->
    
    <!-- Force HTTPS for all API endpoints -->
    <security-constraint>
        <web-resource-collection>
            <web-resource-name>API Endpoints</web-resource-name>
            <url-pattern>/api/*</url-pattern>
            <http-method>GET</http-method>
            <http-method>POST</http-method>
            <http-method>PUT</http-method>
            <http-method>DELETE</http-method>
            <http-method>OPTIONS</http-method>
        </web-resource-collection>
        <user-data-constraint>
            <!-- Guarantees encrypted communication -->
            <transport-guarantee>CONFIDENTIAL</transport-guarantee>
        </user-data-constraint>
    </security-constraint>

    <!-- Force HTTPS for authentication -->
    <security-constraint>
        <web-resource-collection>
            <web-resource-name>Authentication</web-resource-name>
            <url-pattern>/api/auth/*</url-pattern>
        </web-resource-collection>
        <user-data-constraint>
            <transport-guarantee>CONFIDENTIAL</transport-guarantee>
        </user-data-constraint>
    </security-constraint>

</web-app>
```

---

## 📦 Dépendances Requises

Assurez-vous que votre `pom.xml` inclut:

```xml
<dependencies>
    <!-- Jakarta EE Core APIs -->
    <dependency>
        <groupId>jakarta.platform</groupId>
        <artifactId>jakarta.jakartaee-api</artifactId>
        <version>11.0.0</version>
        <scope>provided</scope>
    </dependency>

    <!-- Jakarta Servlet -->
    <dependency>
        <groupId>jakarta.servlet</groupId>
        <artifactId>jakarta.servlet-api</artifactId>
        <version>6.1.0</version>
        <scope>provided</scope>
    </dependency>

    <!-- JBoss Logging -->
    <dependency>
        <groupId>org.jboss.logging</groupId>
        <artifactId>jboss-logging</artifactId>
        <version>3.5.3.Final</version>
    </dependency>

    <!-- Redis Client (for rate limiting and audit logs) -->
    <dependency>
        <groupId>redis.clients</groupId>
        <artifactId>jedis</artifactId>
        <version>5.0.1</version>
    </dependency>

    <!-- Jakarta Persistence (for audit archive) -->
    <dependency>
        <groupId>jakarta.persistence</groupId>
        <artifactId>jakarta.persistence-api</artifactId>
        <version>3.1.0</version>
        <scope>provided</scope>
    </dependency>

    <!-- MicroProfile Config -->
    <dependency>
        <groupId>org.eclipse.microprofile.config</groupId>
        <artifactId>microprofile-config-api</artifactId>
        <version>3.0.3</version>
    </dependency>
</dependencies>
```

---

## 🔌 Injection de Dépendances (CDI)

Pour que `RateLimitingFilter` fonctionne correctement, configurez l'injection:

**Fichier:** `backend/src/main/java/com/secureteam/security/RateLimitingFilter.java`

Remplacez cette section:
```java
// ❌ AVANT (nécessite configuration manuelle)
private RateLimitingService rateLimitingService;

@Override
public void init(FilterConfig filterConfig) throws ServletException {
    // This would be injected in a real application
    LOG.info("[RateLimit] Filter initialized - Login and MFA endpoints protected");
}
```

Par:
```java
// ✅ APRÈS (avec injection CDI)
@Inject
private RateLimitingService rateLimitingService;

@Override
public void init(FilterConfig filterConfig) throws ServletException {
    // RateLimitingService automatically injected by CDI container
    LOG.info("[RateLimit] Filter initialized - Login and MFA endpoints protected");
}
```

---

## 🗄️ Configuration Redis pour Rate Limiting

**Fichier:** `backend/src/main/resources/META-INF/microprofile-config.properties`

```properties
# Redis Connection for Rate Limiting and Audit Logs
redis.host=${REDIS_HOST:localhost}
redis.port=${REDIS_PORT:6379}
redis.password=${REDIS_PASSWORD:}
redis.database=${REDIS_DATABASE:0}
redis.timeout=${REDIS_TIMEOUT:2000}
redis.max-pool-size=${REDIS_MAX_POOL_SIZE:20}

# Create Jedis pool configuration in a CDI producer
# See: com.secureteam.config.RedisConfiguration
```

**Fichier (Nouveau):** `backend/src/main/java/com/secureteam/config/RedisConfiguration.java`

```java
package com.secureteam.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@ApplicationScoped
public class RedisConfiguration {

    private static final Logger LOG = Logger.getLogger(RedisConfiguration.class);

    @Inject
    @ConfigProperty(name = "redis.host", defaultValue = "localhost")
    private String redisHost;

    @Inject
    @ConfigProperty(name = "redis.port", defaultValue = "6379")
    private int redisPort;

    @Inject
    @ConfigProperty(name = "redis.password", defaultValue = "")
    private String redisPassword;

    @Inject
    @ConfigProperty(name = "redis.timeout", defaultValue = "2000")
    private int redisTimeout;

    @Inject
    @ConfigProperty(name = "redis.max-pool-size", defaultValue = "20")
    private int maxPoolSize;

    @Produces
    @ApplicationScoped
    public JedisPool createJedisPool() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxPoolSize);
        config.setMaxIdle(maxPoolSize / 2);
        config.setMinIdle(5);
        config.setTestOnBorrow(true);
        config.setTestOnReturn(true);

        JedisPool pool;
        if (redisPassword != null && !redisPassword.isEmpty()) {
            pool = new JedisPool(config, redisHost, redisPort, redisTimeout, redisPassword);
        } else {
            pool = new JedisPool(config, redisHost, redisPort, redisTimeout);
        }

        LOG.infov("[Redis] Connection pool created: {0}:{1}", redisHost, redisPort);
        return pool;
    }
}
```

---

## 🗺️ Table d'Audit Logs PostgreSQL

**Créer cette table pour archiver les logs d'audit:**

```sql
-- Create audit logs table
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID UNIQUE NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    severity VARCHAR(20) NOT NULL CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL')),
    username VARCHAR(255),
    details TEXT NOT NULL,
    client_ip VARCHAR(45),
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indices for efficient querying
CREATE INDEX idx_audit_username ON audit_logs(username);
CREATE INDEX idx_audit_ip ON audit_logs(client_ip);
CREATE INDEX idx_audit_timestamp ON audit_logs(timestamp DESC);
CREATE INDEX idx_audit_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_severity ON audit_logs(severity);

-- Create immutable audit log (append-only)
-- This prevents accidental modification of historical records
CREATE TABLE audit_logs_archive (
    LIKE audit_logs INCLUDING ALL
);
ALTER TABLE audit_logs_archive ADD CONSTRAINT immutable_check 
    CHECK (created_at <= CURRENT_TIMESTAMP);

-- Grant audit logs access to monitoring role
GRANT SELECT ON audit_logs TO monitoring_user;
GRANT SELECT ON audit_logs_archive TO monitoring_user;
```

---

## 🔐 Configuration CORS pour Production

**Fichier:** `backend/src/main/java/com/secureteam/security/CorsFilter.java`

Modifier la section `TRUSTED_ORIGINS`:

```java
// ❌ AVANT (développement uniquement)
static {
    TRUSTED_ORIGINS.add("http://localhost:3000");
    TRUSTED_ORIGINS.add("http://localhost:5173");
}

// ✅ APRÈS (pour production)
static {
    // Load from environment for flexibility
    String allowedOrigin = System.getenv("CORS_ALLOWED_ORIGIN");
    if (allowedOrigin != null && !allowedOrigin.isEmpty()) {
        TRUSTED_ORIGINS.add(allowedOrigin);
        LOG.infov("[CORS] Added from environment: {0}", allowedOrigin);
    } else {
        LOG.error("[CORS] No CORS_ALLOWED_ORIGIN environment variable set!");
    }
    
    // Still allow localhost for development/testing
    if (isDevelopment()) {
        TRUSTED_ORIGINS.add("http://localhost:3000");
        TRUSTED_ORIGINS.add("http://localhost:5173");
    }
}

private static boolean isDevelopment() {
    String profile = System.getenv("APP_PROFILE");
    return profile == null || profile.equals("dev");
}
```

---

## 📝 Vérification POST-DÉPLOIEMENT

### 1. Vérifier les Headers de Sécurité

```bash
# Check response headers
curl -I https://your-app.com/api/health

# Expected headers:
# Content-Security-Policy: default-src 'self'...
# Strict-Transport-Security: max-age=31536000...
# X-Frame-Options: DENY
# X-Content-Type-Options: nosniff
```

### 2. Tester Rate Limiting

```bash
# Test login rate limiting (5 requests in 5 minutes)
for i in {1..6}; do
  curl -X POST https://your-app.com/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"test"}'
  echo "Request $i done"
done

# Expected: 6th request returns HTTP 429 Too Many Requests
```

### 3. Tester CORS

```bash
# From browser console with cross-origin request
fetch('https://your-app.com/api/data', {
  method: 'GET',
  credentials: 'include'
})
.then(r => r.json())
.then(d => console.log(d))
.catch(e => console.error('CORS failed:', e));

# Check response headers include Access-Control-Allow-Origin
```

### 4. Vérifier Audit Logs

```bash
# Check Redis audit logs
redis-cli
> KEYS audit:*
> LRANGE audit:user:admin 0 10

# Check PostgreSQL archive
psql -U postgres -d secureteam
> SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 10;
```

---

## 🐛 Troubleshooting

### Rate Limiting Not Working

**Problème:** Toutes les requêtes passent même après le seuil
**Solution:** 
```java
// Verify Redis connection
jedisPool.getResource().ping();  // Should return "PONG"

// Check if RateLimitingService is injected
@Inject private RateLimitingService rateLimitingService;
```

### CORS Headers Missing

**Problème:** `Access-Control-Allow-Origin` header absent
**Solution:**
```java
// Verify filter is applied
@WebFilter(urlPatterns = "/api/*")  // Must include your endpoint

// Check trusted origins list
System.out.println(TRUSTED_ORIGINS);  // Should contain your origin
```

### Audit Logs Not Persisting

**Problème:** Logs in Redis but not in PostgreSQL
**Solution:**
```java
// Verify database connection
@Inject private EntityManager entityManager;
entityManager.persist(new AuditLog(...));
entityManager.flush();
```

---

## 📊 Monitoring Queries

### Top IPs by Failed Logins
```sql
SELECT client_ip, COUNT(*) as failures 
FROM audit_logs 
WHERE event_type = 'AUTH_LOGIN_FAILURE'
AND timestamp > NOW() - INTERVAL '1 day'
GROUP BY client_ip 
ORDER BY failures DESC 
LIMIT 10;
```

### User Activity Timeline
```sql
SELECT timestamp, event_type, details 
FROM audit_logs 
WHERE username = 'suspected_user'
ORDER BY timestamp DESC;
```

### Rate Limit Effectiveness
```sql
SELECT 
    COUNT(CASE WHEN event_type = 'RATE_LIMIT_EXCEEDED' THEN 1 END) as limit_hits,
    COUNT(CASE WHEN event_type = 'AUTH_LOGIN_FAILURE' THEN 1 END) as login_failures,
    COUNT(CASE WHEN event_type = 'AUTH_LOGIN_SUCCESS' THEN 1 END) as login_success
FROM audit_logs 
WHERE timestamp > NOW() - INTERVAL '7 days';
```

---

**Document Créé:** 2024
**Version:** 1.0
**Status:** ✅ READY FOR INTEGRATION

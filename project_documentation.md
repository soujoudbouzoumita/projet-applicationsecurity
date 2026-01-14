# SecureTeam Access - Project Documentation

## 1. Project Overview & Logical Flow
**SecureTeam Access** is a highly secure authentication and authorization system designed to demonstrate Zero Trust principles. It separates the user interface (Frontend) from the business logic (Backend), secured by a robust Reverse Proxy and state-of-the-art cryptography.

### Logical Interaction Flow
1.  **User Entry**: The user accesses the application via the **Frontend** (Node.js/Vite/Lit).
2.  **Reverse Proxy**: All API requests are intercepted by **Nginx**, which acts as a gateway, hiding the backend topology and managing header security.
3.  **Authentication (The Gatekeeper)**:
    *   **Login**: User sends credentials to `/api/auth/login`.
    *   **Verification**: Backend hashes the password (PBKDF2) and compares it with the PostgreSQL database.
    *   **MFA**: If valid, the user must provide a TOTP code (Google Authenticator), verified against a secret stored in Redis (setup) or Postgres (persistent).
    *   **Token Issuance**: Upon success, a **PASETO** (Platform-Agnostic Security Tokens) is issued. Unlike JWT, this token is encrypted and tamper-proof by design.
4.  **Authorization (The Guard)**:
    *   **SecurityFilter**: Every subsequent request is intercepted. It validates the PASETO token.
    *   **ABAC Engine**: The request context (User Role, Department, IP Address, Device ID) is evaluated against a Zero Trust Policy to grant or deny access to resources like specific projects or files.
5.  **Data Persistence**:
    *   **Structured Data**: Users, Roles, and Metadata are stored in **PostgreSQL**.
    *   **Session/Cache**: Ephemeral data (MFA setup codes) is stored in **Redis**.
    *   **Unstructured Data**: Files and assets are stored in **MinIO** (S3-compatible storage).

---

## 2. Technology Stack & Versions
This project uses modern, secure, and enterprise-grade components.

| Component | Technology | Version | Justification |
| :--- | :--- | :--- | :--- |
| **Backend Framework** | WildFly (Jakarta EE) | **38.0.1.Final** | Enterprise-grade application server, robust security subsystem, Jakarta EE 11 support. |
| **Language** | Java | **21 (LTS)** | Latest Long-Term Support version, offering Virtual Threads and performance improvements. |
| **Frontend** | Node.js / Vite / Lit | **Node 20 / Lit 3.1** | Lightweight, web-components based standard (Lit) for performance and no-framework lock-in. |
| **Database** | PostgreSQL | **16-alpine** | The most advanced open-source relational database; secure, ACID compliant. |
| **Cache/KV Store** | Redis | **7-alpine** | High-performance session management and rate limiting. |
| **Object Storage** | MinIO | **Latest** | S3-compatible, secure by default, allows isolation of file storage. |
| **Token Standard** | PASETO (Paseto.io) | **v4.public** | **Alternative to JWT**. Chosen because JWTs allow "alg: none" attacks. PASETO forces strong cryptography. |
| **MFA** | TOTP (Aerogear) | **1.0.0** | Time-based One-Time Password. Standard, offline-capable, and more secure than SMS. |

---

## 3. Vulnerabilities & Mitigations
The core purpose of this project is to mitigate common OWASP Top 10 vulnerabilities.

### A. Broken Authentication (Credential Stuffing / Weak Passwords)
*   **Vulnerability**: Attackers use lists of stolen passwords or guess simple ones.
*   **Mitigation**:
    *   **PBKDF2 Hashing**: Passwords are **never** stored in plain text. They are hashed using `PBKDF2WithHmacSHA512` with a unique random **Salt** per user. This makes Rainbow Table attacks impossible.
    *   **MFA (Multi-Factor Auth)**: Even if a password is stolen, the attacker cannot login without the physical device generating TOTP codes.
    *   **Strict Policy**: The backend enforces 12+ chars, mixed case, and special characters.

### B. Broken Access Control (IDOR / Privilege Escalation)
*   **Vulnerability**: A user changes an ID in the URL (e.g., `/users/1` to `/users/2`) to access another user's data.
*   **Mitigation**:
    *   **ABAC (Attribute-Based Access Control)**: We do not rely solely on simple Roles (RBAC). Access is decided based on attributes (Department, Project assignment).
    *   **Context Awareness**: The `SecurityFilter` strictly evaluates every request. If a user in "HR" tries to access "Engineering" data, valid credentials won't help—access is denied.

### C. Security Misconfiguration
*   **Vulnerability**: Default accounts, open ports, verbose error messages.
*   **Mitigation**:
    *   **Reverse Proxy**: The backend port `8080` is customizable but hidden behind Nginx.
    *   **Environment Variables**: Secrets (DB passwords, Encryption Keys) are injected via `.env`, never hardcoded in `pom.xml` or source code (12-Factor App methodology).
    *   **Seeding**: The `DataSeeder` ensures a secure default state (`admin` user exists) so initial setup isn't left open.

### D. Sensitive Data Exposure / Session Hijacking
*   **Vulnerability**: Stealing a session cookie to impersonate a user.
*   **Mitigation**:
    *   **PASETO Tokens**: Signed tokens ensure integrity. If an attacker modifies the token, it becomes invalid.
    *   **Short TTL**: Tokens have a short lifespan.
    *   **Validation Endpoint**: The frontend proactively checks `/api/auth/validate`. If a session is invalid or revoked server-side, the frontend nukes the session immediately (as demonstrated in our recent fix).

---

## 4. Design Decisions & Alternatives

### Why PASETO instead of JWT?
*   **JWT (JSON Web Tokens)**: The industry standard but flawed. It allows developers to choose the algorithm, leading to the infamous `alg: none` vulnerability where attackers strip the signature.
*   **PASETO**: "Platform-Agnostic Security Tokens". It removes the choice. You *must* use strong encryption. It is versioned (v4), so if crypto breaks, we just bump the version, we don't patch the algorithm.

### Why WildFly (Java EE) instead of Spring Boot?
*   **Spring Boot**: Excellent for microservices but opinionated.
*   **WildFly**: Follows strict **Jakarta EE standards**. This ensures our code is portable and uses standard APIs (JAX-RS, JPA, CDI) rather than framework-specific magic. It also isolates the application from the server runtime, providing a smaller attack surface for the application code itself.

### Why PostgreSQL + Redis?
*   **Alternative**: Keeping sessions in memory or just using the database.
*   **Justification**:
    *   **Redis** is O(1) fast. Checking a revoked token or an MFA code against a SQL DB on every request would slow down the system (DDoS vulnerability). Redis handles this load effortlessly.
    *   **PostgreSQL** ensures data integrity (ACID) for user accounts, which is non-negotiable for security data.

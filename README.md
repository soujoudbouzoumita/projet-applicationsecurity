🛡️ SecureTeam Access
Expert Edition – Zero Trust Identity & Access Broker

Next-Generation Zero Trust Identity & Access Management (IAM) for Critical and Cloud-Native Infrastructures










📌 Project Overview

SecureTeam Access (SentinelKey) is a high-security Access Broker designed for modern infrastructures where implicit trust is no longer acceptable.

The platform enforces a Zero Trust Architecture (ZTA) based on:

Continuous authentication

Just-In-Time (JIT) access

Attribute-Based Access Control (ABAC)

Strong cryptographic primitives

Every access request is dynamically evaluated based on identity, context, device, and environment, making the system resilient against credential theft, replay attacks, and lateral movement.

🧠 Security Architecture
1️⃣ Multi-Factor Authentication – TOTP (RFC 6238)

Time-Based One-Time Password authentication

Secure secret generation via SecureRandom

Base32 encoding with QR Code provisioning (ZXing)

Temporary secret storage in Redis (TTL: 10 min)

Controlled time-drift tolerance to balance usability and security

Goal: Prevent credential-based attacks and enforce strong user verification.

2️⃣ Token Security – PASETO v2 Public (JWT Alternative)

JWT is intentionally not used due to its historical weaknesses.

SecureTeam Access uses:

PASETO v2.public

Asymmetric cryptography (Ed25519)

Signature validation via Bouncy Castle

Token revocation using JTI stored in Redis

✔ Prevents:

Token forgery

Algorithm confusion attacks

Replay attacks

3️⃣ Attribute-Based Access Control (ABAC)

Access decisions are computed dynamically using:

Dimension	Examples
Subject	Roles, department, authorized projects
Resource	Project ID, requested action
Environment	Device ID, IP address, business hours

This enables fine-grained, context-aware authorization instead of static role checks.

4️⃣ Steganography Module (Data Concealment)

An advanced LSB-based steganography service is included for secure data concealment.

Supports PNG and BMP (lossless)

Bit-level embedding across RGB channels

Length-prefixed payload encoding

REST endpoints:

/api/stego/hide

/api/stego/extract

Designed for security experimentation and controlled data exfiltration scenarios.

📂 Project Structure
SentinelKey-Access-Broker/
├── frontend/                  # Secure Web Dashboard (Lit PWA)
│   └── src/
│       ├── services/          # Secure API client & token handling
│       └── secureteam-app.js  # Main UI logic
│
├── backend/                   # Jakarta EE Security Engine
│   └── src/main/java/com/secureteam/
│       ├── auth/              # MFA, PASETO, ABAC
│       ├── api/               # REST Resources
│       ├── model/             # Domain entities
│       └── steganography/     # LSB Engine
│
├── infrastructure/            # CI/CD & environment config
└── docker-compose.prod.yml    # Production deployment

🛠️ Technology Stack
Backend

Jakarta EE 11 – Enterprise-grade security

WildFly 38

Bouncy Castle – Cryptography (Ed25519)

Redis 7.2 – MFA secrets & token revocation

PostgreSQL

Frontend

Lit 3.x

Secure API client with automatic token injection

Device fingerprinting

🔐 API Overview
Endpoint	Method	Security
/auth/register	POST	Public
/auth/mfa/verify	POST	Public
/projects	GET	ABAC
/stego/hide	POST	PASETO
/audit	GET	ADMIN
/keys/public	GET	Public
🚀 Deployment
Local
cd backend
mvn clean package wildfly:run

cd frontend
npm install
npm run dev

Production
docker-compose -f docker-compose.prod.yml up -d --build

🛡️ Security Improvements & CVE Fixes
Dependency	Old	New	CVEs Fixed
nimbus-jose-jwt	1.0.0	10.0.2	CVE-2025-53864, CVE-2019-17195
postgresql	42.7.2	42.7.8	CVE-2024-1597
bcprov	1.77	1.78	CVE-2024-29857
jedis	5.1.2	5.2.0	Security enhancements
⚠️ Identified Vulnerabilities (Audit)
1. Hardcoded Secrets (CWE-798) – Critical

Credentials found in source code and config files.

2. Sensitive Log Disclosure (CWE-209) – Medium

Use of System.out.println() and printStackTrace().

3. Weak TOTP Implementation (CWE-327) – High

Manual crypto logic and excessive time window.

4. Deprecated Dependencies (CWE-1104) – Medium
🔧 Proposed Remediations

Externalize secrets using @ConfigProperty

Replace console logs with JBoss Logging

Harden TOTP validation window (±1 min)

Secure and document manual crypto implementation

Dependency upgrades and verification

✅ Verification Plan
Automated
mvn clean compile
mvn test

Manual

Verify logs do not expose secrets

Validate MFA enrollment & verification

Confirm token revocation behavior

📜 License & Versioning

Version: 3.1.0 – Integration Edition

License: Proprietary

Owner: SecureTeam Application Security

Generated with 🛡️ by SecureTeam Security Suite
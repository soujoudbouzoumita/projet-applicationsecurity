# Security Hardening – TLS 1.3, HSTS, CAA

---

## Overview

This document records the **exact security hardening applied** on the WildFly runtime.
It is intended to be versioned in Git and reflects **what is deployed**, not a tutorial.

Changes covered:

* Enforced **TLS 1.3 only** at the application server level
* Enabled **HSTS** via Undertow response headers
* Restricted certificate issuance using **CAA DNS records** (Namecheap)

---

## TLS 1.3 Enforcement (WildFly 38.0.1)

### Keystore

* Type: JKS
* Location: `$WILDFLY_HOME/standalone/configuration/keystore.jks`
* Key algorithm: RSA 2048

Keystore is referenced via Elytron and **no legacy TLS protocols are enabled**.

### Elytron SSL Context

Configured in `standalone.xml`:

```xml
<tls>
    <key-stores>
        <key-store name="AppKeyStore">
            <credential-reference clear-text="changeit"/>
            <implementation type="JKS"/>
            <file path="keystore.jks" relative-to="jboss.server.config.dir"/>
        </key-store>
    </key-stores>

    <key-managers>
        <key-manager name="AppKeyManager" key-store="AppKeyStore">
            <credential-reference clear-text="changeit"/>
        </key-manager>
    </key-managers>

    <server-ssl-contexts>
        <server-ssl-context name="SSLContext"
            key-manager="AppKeyManager"
            protocols="TLSv1.3"/>
    </server-ssl-contexts>
</tls>
```

### HTTPS Listener Binding

```xml
<https-listener name="https"
    socket-binding="https"
    ssl-context="SSLContext"/>
```

**Result:** Only TLS 1.3 handshakes are accepted. TLS 1.0/1.1/1.2 are rejected.

---

## HSTS (HTTP Strict Transport Security)

HSTS is enforced at the Undertow layer after HTTPS stability was confirmed.

### Undertow Response Header

```xml
<response-header name="HSTS">
    <header-name>Strict-Transport-Security</header-name>
    <header-value>max-age=31536000; includeSubDomains; preload</header-value>
</response-header>
```

### Filter Attachment

```xml
<host name="default-host">
    <filter-ref name="HSTS"/>
</host>
```

**Effect:** Browsers are forced to use HTTPS for `applicationsec.me` and all subdomains for 1 year.

---

## CAA – Certificate Authority Authorization

CAA is configured at the DNS level (Namecheap) to restrict certificate issuance.

### Domain

`applicationsec.me`

### Active CAA Records

| Flag | Tag       | Value                            |
| ---- | --------- | -------------------------------- |
| 0    | issue     | "letsencrypt.org"                |
| 0    | issuewild | ";"                              |
| 0    | iodef     | "mailto:admin@applicationsec.me" |

**Policy:**

* Only Let’s Encrypt is authorized
* Wildcard certificates are explicitly disallowed
* Incident notifications are routed via email

---

## Verification

### TLS Protocol Validation

```bash
openssl s_client -connect applicationsec.me:443 -tls1_3
```

Expected: successful handshake using TLSv1.3

### HSTS Header Check

```bash
curl -I https://applicationsec.me
```

Expected:

```
Strict-Transport-Security: max-age=31536000; includeSubDomains; preload
```

### CAA Resolution

```bash
dig applicationsec.me CAA
```

---

## Security Status

* TLS: **TLS 1.3 only**
* Transport: **HTTPS enforced**
* Browser policy: **HSTS enabled**
* Certificate issuance: **Restricted (CAA)**

This configuration aligns with modern AppSec and OWASP ASVS transport requirements.

---


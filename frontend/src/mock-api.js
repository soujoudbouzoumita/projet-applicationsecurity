export class MockAPI {
  static async health() {
    return {
      status: "UP",
      message: "SecureTeam Access Engine is running."
    };
  }

  static async setupMfa(username) {
    const secret = "JBSWY3DPEBLW64TMMQ======";
    const qrUri = otpauth://totp/SecureTeamAccess:${username}?secret=${secret}&issuer=SecureTeamAccess;
    
    try {
      const { default: QRCodeLib } = await import('qrcode');
      const qrImage = await QRCodeLib.toDataURL(qrUri, {
        width: 200,
        margin: 2,
        color: {
          dark: '#000000',
          light: '#ffffff'
        }
      });
      
      return {
        secret: secret,
        qrUri: qrUri,
        qrImage: qrImage
      };
    } catch (err) {
      console.error('QR Code generation error:', err);
      return {
        secret: secret,
        qrUri: qrUri,
        qrImage: "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
      };
    }
  }

  static async verifyMfa(username, code) {
    if (/^\d{6}$/.test(code)) {
      return {
        token: "v2.public.eyJkYXRhIjoiZm9vYmFyIiwiaWF0IjoiMjAxNC0wOC0yNFQyMDowMjowMVoifQ_QGm7QTPHZ_H2t6xRf-OkNr5zD5f9bQ1-yFDvKpHnmCXW4",
        message: "MFA verified successfully"
      };
    }
    throw new Error("Invalid code");
  }

  static async getAllUsers() {
    return [
      {
        id: 1,
        username: "admin",
        department: "Security",
        roles: ["security_admin"],
        authorizedProjects: ["Project Alpha", "Project Beta"]
      },
      {
        id: 2,
        username: "dev_user",
        department: "Engineering",
        roles: ["external_collaborator"],
        authorizedProjects: ["Project Alpha"]
      },
      {
        id: 3,
        username: "analyst",
        department: "Operations",
        roles: ["external_collaborator"],
        authorizedProjects: ["Project Beta"]
      }
    ];
  }

  static async getUser(id) {
    const users = await this.getAllUsers();
    return users.find(u => u.id === id);
  }

  static async getProjects() {
    return [
      {
        id: 1,
        name: "Project Alpha",
        description: "Zero Trust Infrastructure",
        owner: "admin",
        status: "active"
      },
      {
        id: 2,
        name: "Project Beta",
        description: "Security Audit System",
        owner: "admin",
        status: "active"
      }
    ];
  }

  static async getAuditLogs() {
    return [
      {
        id: 1,
        timestamp: new Date().toISOString(),
        user: "admin",
        action: "LOGIN",
        resource: "SecureTeam Access",
        result: "SUCCESS"
      },
      {
        id: 2,
        timestamp: new Date(Date.now() - 3600000).toISOString(),
        user: "dev_user",
        action: "PROJECT_ACCESS",
        resource: "Project Alpha",
        result: "SUCCESS"
      }
    ];
  }
}

const originalFetch = window.fetch;
window.fetch = async (url, options = {}) => {
  try {
    console.log("Fetch request:", url);
    
    if (url.includes("/api/auth/health")) {
      console.log("Handling health check");
      return new Response(JSON.stringify(await MockAPI.health()), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    }
    
    if (url.includes("/api/auth/mfa/setup")) {
      console.log("Handling MFA setup");
      const fullUrl = new URL(url, window.location.origin);
      const username = fullUrl.searchParams.get("username");
      const result = await MockAPI.setupMfa(username);
      return new Response(JSON.stringify(result), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    }
    
    if (url.includes("/api/auth/mfa/verify")) {
      console.log("Handling MFA verify");
      const body = JSON.parse(options.body || "{}");
      try {
        const result = await MockAPI.verifyMfa(body.username, body.code);
        return new Response(JSON.stringify(result), {
          status: 200,
          headers: { "Content-Type": "application/json" }
        });
      } catch (err) {
        return new Response(JSON.stringify({ error: "Invalid OTP code" }), {
          status: 401,
          headers: { "Content-Type": "application/json" }
        });
      }
    }
    
    if (url.includes("/api/users")) {
      return new Response(JSON.stringify(await MockAPI.getAllUsers()), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    }
    
    if (url.includes("/api/projects")) {
      return new Response(JSON.stringify(await MockAPI.getProjects()), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    }
    
    if (url.includes("/api/audit")) {
      return new Response(JSON.stringify(await MockAPI.getAuditLogs()), {
        status: 200,
        headers: { "Content-Type": "application/json" }
      });
    }
  } catch (error) {
    console.error("Mock API Error:", error);
    return new Response(JSON.stringify({ error: error.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" }
    });
  }
  
  return originalFetch(url, options);
};

console.log("Mock API loaded");
export class AuthService {

    constructor() {
        this.token = localStorage.getItem('secureteam_token');
    }

    async checkAuth() {
        if (!this.token) return false;
        try {
            // Verify token with backend
            const headers = this._getHeaders();
            const resp = await fetch('/api/auth/validate', { headers });

            if (resp.ok) {
                return true;
            } else {
                console.warn("Token validation failed, logging out");
                this.logout();
                return false;
            }
        } catch (e) {
            console.error("Auth check error", e);
            // If backend is down, we might want to stay logged in or strict logout?
            // For security, strictly logout or assume offline.
            // Let's assume offline but valid if formatted correctly? 
            // No, failsafe: logout if uncertain to prevent unauthorized access locally.
            this.logout();
            return false;
        }
    }

    async login(username, password) {
        const resp = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!resp.ok) {
            throw new Error("Invalid credentials");
        }

        return await resp.json(); // Returns { status: "MFA_REQUIRED" | "MFA_SETUP_REQUIRED" }
    }

    async register(username, password, department) {
        const resp = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password, department })
        });

        if (!resp.ok) {
            const err = await resp.text();
            throw new Error(err || "Registration failed");
        }
        return true;
    }

    async verifyMfa(username, code) {
        const resp = await fetch('/api/auth/mfa/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, code })
        });

        if (resp.ok) {
            const data = await resp.json();
            this.token = data.token;
            localStorage.setItem('secureteam_token', data.token);
            return true;
        } else {
            throw new Error('MFA Verification failed');
        }
    }

    async logout() {
        this.token = null;
        localStorage.removeItem('secureteam_token');
    }
}

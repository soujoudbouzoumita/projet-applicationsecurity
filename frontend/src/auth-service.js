export class AuthService {

    constructor() {
        this.token = localStorage.getItem('secureteam_token');
<<<<<<< HEAD
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
=======
        // NOTE: In production, use encrypted IndexedDB or opaque HTTP-only cookies if possible.
        // localStorage is used here for zero-dependency simplicity in this generated file.
    }

    async checkAuth() {
        return !!this.token;
    }

    async login(username, password) {
        // CALL REAL BACKEND 1FA LOGIN
        const resp = await fetch('/secureteam-access/api/auth/login', {
>>>>>>> origin/main
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!resp.ok) {
<<<<<<< HEAD
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
=======
            const errorText = await resp.text();
            throw new Error(errorText || "Invalid credentials");
        }

        const data = await resp.json();
        // data contains { username, mfaEnabled }
        return data;
>>>>>>> origin/main
    }

    async logout() {
        this.token = null;
        localStorage.removeItem('secureteam_token');
    }
<<<<<<< HEAD
=======

    generateCodeVerifier() {
        const array = new Uint32Array(56 / 2);
        window.crypto.getRandomValues(array);
        return Array.from(array, dec => ('0' + dec.toString(16)).substr(-2)).join('');
    }

    async generateCodeChallenge(verifier) {
        const encoder = new TextEncoder();
        const data = encoder.encode(verifier);
        const digest = await window.crypto.subtle.digest("SHA-256", data);

        return btoa(String.fromCharCode(...new Uint8Array(digest)))
            .replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
    }
>>>>>>> origin/main
}

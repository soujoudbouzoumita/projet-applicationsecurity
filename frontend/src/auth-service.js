export class AuthService {

    constructor() {
        this.token = localStorage.getItem('secureteam_token');
        // NOTE: In production, use encrypted IndexedDB or opaque HTTP-only cookies if possible.
        // localStorage is used here for zero-dependency simplicity in this generated file.
    }

    async checkAuth() {
        return !!this.token;
    }

    async login(username, password) {
        // CALL REAL BACKEND 1FA LOGIN
        const resp = await fetch('/secureteam-access/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password })
        });

        if (!resp.ok) {
            const errorText = await resp.text();
            throw new Error(errorText || "Invalid credentials");
        }

        const data = await resp.json();
        // data contains { username, mfaEnabled }
        return data;
    }

    async logout() {
        this.token = null;
        localStorage.removeItem('secureteam_token');
    }

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
}

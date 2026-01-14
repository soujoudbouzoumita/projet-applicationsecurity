import { LitElement, html, css } from 'lit';
import { AuthService } from './auth-service.js';
<<<<<<< HEAD

=======
import './mock-api.js'; // Mock API pour test sans backend
>>>>>>> origin/main

export class SecureTeamApp extends LitElement {
  static properties = {
    isAuthenticated: { type: Boolean },
    loginStep: { type: Number }, // 1: Credentials, 2: MFA
    userRole: { type: String }, // 'external_collaborator' or 'security_admin'
    remainingTime: { type: String },
    projects: { type: Array },
    activeView: { type: String },
    qrImage: { type: String },
    pendingUser: { type: String },
    backendStatus: { type: String }, // 'checking', 'online', 'offline'
    pendingRequests: { type: Array },
    selectedRequest: { type: Object },
    approvalComment: { type: String },
    requestResource: { type: String },
    requestWindow: { type: String },
<<<<<<< HEAD
    requestJustification: { type: String },
    isRegistering: { type: Boolean }
=======
    requestJustification: { type: String }
>>>>>>> origin/main
  };

  constructor() {
    super();
    this.authService = new AuthService();
    this.isAuthenticated = false;
    this.loginStep = 1;
    this.userRole = 'external_collaborator';
    this.remainingTime = "1h 45m";
    this.projects = ["Project Alpha", "Project Beta"];
    this.activeView = "dashboard";
    this.qrImage = "";
    this.pendingUser = "";
    this.backendStatus = "checking";
<<<<<<< HEAD
    this.isRegistering = false;

=======
    
>>>>>>> origin/main
    // Charger les requests depuis localStorage ou utiliser les valeurs par défaut
    const savedRequests = localStorage.getItem('secureteam_requests');
    if (savedRequests) {
      this.pendingRequests = JSON.parse(savedRequests);
    } else {
      this.pendingRequests = [
        {
          id: 1,
          username: "user_alpha",
          requestType: "JIT Request (2h)",
          timestamp: "14:12:05",
          description: "Requesting temporary elevated privileges for Project Alpha",
          status: "PENDING"
        },
        {
          id: 2,
          username: "user_gamma",
          requestType: "Vault Access",
          timestamp: "13:45:22",
          description: "Requesting decryption key access for sensitive data",
          status: "PENDING"
        }
      ];
      // Sauvegarder les données initiales
      localStorage.setItem('secureteam_requests', JSON.stringify(this.pendingRequests));
    }
<<<<<<< HEAD

=======
    
>>>>>>> origin/main
    this.selectedRequest = null;
    this.approvalComment = "";
    this.requestResource = "Infrastucture - Cloud Console [R/W]";
    this.requestWindow = "1 Hour (Fast-track Fix)";
    this.requestJustification = "";
  }

  static styles = css`
    :host {
      display: block;
      min-height: 100vh;
      font-family: 'Outfit', sans-serif;
      background: radial-gradient(circle at top left, #0f172a, #1e1b4b);
      color: #e2e8f0;
    }
    header {
      background: rgba(15, 23, 42, 0.8);
      backdrop-filter: blur(12px);
      padding: 1rem 2rem;
      display: flex;
      justify-content: space-between;
      align-items: center;
      border-bottom: 1px solid rgba(255, 255, 255, 0.1);
      position: sticky;
      top: 0;
      z-index: 100;
    }
    .brand {
      font-size: 1.4rem;
      font-weight: 800;
      letter-spacing: -0.5px;
      background: linear-gradient(90deg, #38bdf8, #818cf8);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    main {
      padding: 2rem;
      max-width: 1200px;
      margin: 0 auto;
    }
    .card {
      background: rgba(30, 41, 59, 0.5);
      border-radius: 20px;
      padding: 2rem;
      border: 1px solid rgba(255, 255, 255, 0.08);
      backdrop-filter: blur(8px);
      transition: all 0.3s ease;
    }
    .card:hover {
      border-color: rgba(56, 189, 248, 0.3);
      background: rgba(30, 41, 59, 0.7);
    }
    .mfa-box {
        text-align: center;
        padding: 1rem;
    }
    .qr-container {
        margin: 1rem auto;
        padding: 1.5rem;
        background: white;
        border-radius: 15px;
        display: inline-block;
        box-shadow: 0 10px 30px rgba(0,0,0,0.5);
    }
    .qr-container img {
        display: block;
        width: 180px;
        height: 180px;
    }
    .otp-input {
        letter-spacing: 1rem;
        font-size: 2rem;
        text-align: center;
        width: 250px;
        margin: 1.5rem auto;
        font-family: monospace;
        background: #0f172a;
        border: 2px solid #38bdf8;
        color: white;
        border-radius: 12px;
        padding: 1rem;
    }
    .status-badge {
      padding: 0.4rem 0.8rem;
      border-radius: 8px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
    }
    .badge-admin { background: rgba(244, 63, 94, 0.2); color: #fb7185; }
    .badge-collab { background: rgba(56, 189, 248, 0.2); color: #7dd3fc; }

    .timer {
      font-size: 3rem;
      font-weight: 800;
      color: #38bdf8;
      text-shadow: 0 0 20px rgba(56, 189, 248, 0.3);
    }
    .login-container {
      display: flex;
      justify-content: center;
      align-items: center;
      height: 90vh;
    }
    .login-card {
      width: 100%;
      max-width: 450px;
      padding: 3rem;
    }
    input, select, textarea {
      width: 100%;
      padding: 1rem;
      margin: 0.8rem 0;
      background: #0f172a;
      border: 1px solid rgba(255, 255, 255, 0.1);
      border-radius: 12px;
      color: white;
      box-sizing: border-box;
      outline: none;
    }
    input:focus { border-color: #38bdf8; }
    
    button {
      padding: 1rem 1.5rem;
      border-radius: 12px;
      border: none;
      cursor: pointer;
      font-weight: 700;
      transition: all 0.2s ease;
    }
    .btn-primary {
      background: #38bdf8;
      color: #0f172a;
      width: 100%;
    }
    .btn-primary:hover { transform: translateY(-1px); box-shadow: 0 5px 15px rgba(56, 189, 248, 0.4); }
    
    .btn-ghost {
      background: transparent;
      color: #94a3b8;
      border: 1px solid rgba(255, 255, 255, 0.1);
    }
    .btn-ghost:hover { background: rgba(255, 255, 255, 0.05); }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(350px, 1fr));
      gap: 1.5rem;
    }
    .tag {
      background: rgba(255, 255, 255, 0.05);
      padding: 0.4rem 0.8rem;
      border-radius: 6px;
      margin: 0.3rem;
      display: inline-block;
      font-size: 0.85rem;
    }
  `;

  async firstUpdated() {
    this.isAuthenticated = await this.authService.checkAuth();
    this._checkBackendHealth();
    setInterval(() => this._checkBackendHealth(), 5000);
  }

  async _checkBackendHealth() {
    try {
      console.log('🔍 Checking backend health...');
      const resp = await fetch('/api/auth/health');
      if (resp.ok) {
        this.backendStatus = 'online';
        console.log('✅ Backend is online');
      } else {
        this.backendStatus = 'offline';
        console.log('❌ Backend returned non-ok status');
      }
    } catch (e) {
      this.backendStatus = 'offline';
      console.error('❌ Backend health check failed:', e);
    }
  }

  render() {
    if (!this.isAuthenticated) {
      return html`
            <div class="login-container">
                <div class="card login-card">
                    <div class="brand" style="margin-bottom: 0.5rem; font-size: 1.8rem;">SecureTeam Access</div>
                    ${this.backendStatus === 'offline' ? html`<div style="padding: 0.5rem; background: rgba(244, 63, 94, 0.1); color: #fb7185; border-radius: 8px; font-size: 0.8rem; margin-bottom: 1.5rem; text-align: center; border: 1px solid rgba(244, 63, 94, 0.3);">⚠️ Security Engine Offline - Please wait for backend to start...</div>` : ''}
                    ${this.backendStatus === 'checking' ? html`<div style="padding: 0.5rem; background: rgba(56, 189, 248, 0.1); color: #38bdf8; border-radius: 8px; font-size: 0.8rem; margin-bottom: 1.5rem; text-align: center;">Checking Security Engine...</div>` : ''}
                    
<<<<<<< HEAD
                    ${this.loginStep === 1 ? (this.isRegistering ? this._renderRegisterForm() : this._renderLoginForm()) : this._renderMFAForm()}
=======
                    ${this.loginStep === 1 ? this._renderLoginForm() : this._renderMFAForm()}
>>>>>>> origin/main
                </div>
            </div>
            `;
    }

    return html`
      <header>
        <div class="brand">SecureTeam Access</div>
        <div style="display: flex; gap: 0.8rem; align-items: center;">
            <span class="status-badge ${this.userRole === 'security_admin' ? 'badge-admin' : 'badge-collab'}">
                ${this.userRole === 'security_admin' ? 'ADMIN MODE' : 'Consultant'} Mode
            </span>
            <button class="btn-ghost" @click="${() => this.activeView = 'dashboard'}">Dashboard</button>
<<<<<<< HEAD
            ${this.userRole === 'security_admin'
        ? html`<button class="btn-ghost" @click="${() => this.activeView = 'responses'}">Responses</button>`
        : html`
=======
            ${this.userRole === 'security_admin' 
              ? html`<button class="btn-ghost" @click="${() => this.activeView = 'responses'}">Responses</button>` 
              : html`
>>>>>>> origin/main
                <button class="btn-ghost" @click="${() => this.activeView = 'request'}">Submit Request</button>
                <button class="btn-ghost" @click="${() => this.activeView = 'myRequests'}">My Requests</button>
              `}
            <button class="btn-ghost" style="color: #fb7185;" @click="${this._logout}">Logout</button>
        </div>
      </header>
      
      <main>
        <h2 style="margin-bottom: 2rem;">Welcome, <span style="color: #38bdf8;">${this.userRole === 'security_admin' ? 'Administrator' : 'Freelance Partner'}</span></h2>
        ${this.activeView === 'dashboard' ? this._renderDashboard() : (this.activeView === 'responses' ? this._renderResponses() : (this.activeView === 'myRequests' ? this._renderMyRequests() : this._renderRequestForm()))}
      </main>
    `;
  }

  _renderLoginForm() {
    return html`
<<<<<<< HEAD
            <h3 style="margin-bottom: 1.5rem; text-align: center;">Secure Login</h3>
            <input type="text" placeholder="Username" id="user">
            <input type="password" placeholder="Password" id="pass">
            <button class="btn-primary" style="margin-top: 1rem;" ?disabled="${this.backendStatus !== 'online'}" @click="${this._handleLoginClick}">Sign In</button>
            <p style="font-size: 0.8rem; color: #94a3b8; text-align: center; margin-top: 1.5rem;">
                New here? <a href="#" style="color: #38bdf8;" @click="${(e) => { e.preventDefault(); this.isRegistering = true; }}">Create an account</a>
=======
            <label style="font-size: 0.8rem; color: #94a3b8;">User Identity</label>
            <input type="text" placeholder="Username" id="user" value="admin">
            <input type="password" placeholder="Password" id="pass" value="password">
            <button class="btn-primary" ?disabled="${this.backendStatus !== 'online'}" @click="${this._toMFAStep}">Next Step: Verify MFA</button>
            <p style="font-size: 0.75rem; color: #64748b; margin-top: 1rem; text-align: center;">
                Zero Trust Verification: Step 1 of 2
>>>>>>> origin/main
            </p>
        `;
  }

<<<<<<< HEAD
  _renderRegisterForm() {
    return html`
              <h3 style="margin-bottom: 1.5rem; text-align: center;">Create Account</h3>
              <input type="text" placeholder="Username (3-50 chars)" id="reg-user">
              <input type="password" placeholder="Password (Strong: 12+ chars, Upper, Lower, Digit, Special)" id="reg-pass">
              <select id="reg-dept">
                  <option value="" disabled selected>Select Department</option>
                  <option value="engineering">Engineering</option>
                  <option value="security">Security Operations</option>
                  <option value="hr">Human Resources</option>
                  <option value="legal">Legal</option>
              </select>
              <button class="btn-primary" style="margin-top: 1rem;" ?disabled="${this.backendStatus !== 'online'}" @click="${this._register}">Register</button>
              <p style="font-size: 0.8rem; color: #94a3b8; text-align: center; margin-top: 1.5rem;">
                  Already have an account? <a href="#" style="color: #38bdf8;" @click="${(e) => { e.preventDefault(); this.isRegistering = false; }}">Sign In</a>
              </p>
          `;
  }

=======
>>>>>>> origin/main
  _renderMFAForm() {
    return html`
            <div class="mfa-box">
                <h3 style="margin-bottom: 0.5rem;">Two-Factor Authentication</h3>
                <p style="color: #94a3b8; font-size: 0.85rem;">Scan this code with Google Authenticator to setup your device.</p>
                
                <div class="qr-container">
                    ${this.qrImage ? html`<img src="${this.qrImage}" alt="MFA QR Code">` : html`<div style="width:180px;height:180px;background:#eee;display:flex;align-items:center;justify-content:center;color:#666">Generating...</div>`}
                </div>

                <p style="color: #94a3b8; font-size: 0.8rem; margin: 1rem 0;">Enter the 6-digit code from your app</p>
                <input type="text" maxlength="6" class="otp-input" id="otp" placeholder="000 000" autofocus>
                
                <button class="btn-primary" @click="${this._login}">Establish Secure Session</button>
                <button class="btn-ghost" style="margin-top: 1rem; width: 100%;" @click="${() => this.loginStep = 1}">Back</button>
            </div>
        `;
  }

  _renderDashboard() {
    if (this.userRole === 'security_admin') {
      return this._renderAdminDashboard();
    }
    return this._renderCollabDashboard();
  }

  _renderCollabDashboard() {
    return html`
            <div class="grid">
                <div class="card">
                    <span class="status-badge badge-collab">Mission Pulse</span>
                    <h3 style="margin: 1rem 0;">JIT Session TTL</h3>
                    <div class="timer">${this.remainingTime}</div>
                    <p style="font-size: 0.85rem; color: #94a3b8; line-height: 1.5;">
                        Your access is temporary. Session will terminate and PASETO token will be revoked automatically at the end of the window.
                    </p>
                </div>

                <div class="card">
                    <h3 style="margin-bottom: 1.5rem;">Access Scope (ABAC)</h3>
                    <p style="font-size: 0.85rem; color: #94a3b8; margin-bottom: 1rem;">Authorized Projects:</p>
                    <div style="margin-bottom: 1.5rem;">
                        ${this.projects.map(p => html`<span class="tag">${p}</span>`)}
                    </div>
                    <div style="border-top: 1px solid rgba(255,255,255,0.05); padding-top: 1rem;">
                        <p style="font-size: 0.75rem; color: #64748b;">Device ID: <span style="color: #e2e8f0;">SEC-WS-442</span></p>
                        <p style="font-size: 0.75rem; color: #64748b;">IP Address: <span style="color: #e2e8f0;">10.0.5.12 (VPN)</span></p>
                    </div>
                </div>
            </div>

            <div class="card" style="margin-top: 1.5rem;">
                <h3 style="margin-bottom: 1rem;">Recent Activity</h3>
                <div style="font-size: 0.85rem;">
                    <div style="padding: 0.8rem 0; border-bottom: 1px solid rgba(255,255,255,0.05); display: flex; justify-content: space-between;">
                        <span>Decrypted sensitive key for Project Alpha</span>
                        <span style="color: #64748b;">2 mins ago</span>
                    </div>
                    <div style="padding: 0.8rem 0; display: flex; justify-content: space-between;">
                        <span>Session extended via JIT request #882</span>
                        <span style="color: #64748b;">1 hour ago</span>
                    </div>
                </div>
            </div>
        `;
  }

  _renderAdminDashboard() {
    return html`
            <div class="grid">
                <div class="card">
                    <h3 style="margin-bottom: 1rem;">System Health</h3>
                    <div style="display: flex; gap: 2rem;">
                        <div>
                            <div style="font-size: 2rem; font-weight: 700;">12</div>
                            <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase;">Active Sessions</div>
                        </div>
                        <div>
                            <div style="font-size: 2rem; font-weight: 700; color: #facc15;">${this.pendingRequests.filter(r => r.status === 'PENDING').length}</div>
                            <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase;">Pending Approvals</div>
                        </div>
                    </div>
                </div>

                <div class="card">
                    <h3 style="margin-bottom: 1rem;">Threat Prevention</h3>
                    <div style="padding: 1rem; background: rgba(244, 63, 94, 0.1); border-radius: 12px; border: 1px solid rgba(244, 63, 94, 0.2);">
                        <p style="color: #fb7185; font-size: 0.85rem; font-weight: 600;">Warning: Abnormal Activity Detected</p>
                        <p style="font-size: 0.75rem; color: #e2e8f0; margin-top: 0.3rem;">JTI Replay attempt from 192.168.1.100 blocked by Redis.</p>
                    </div>
                </div>
            </div>

            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; margin-top: 1.5rem;">
                <div class="card">
                    <h3 style="margin-bottom: 1rem;">Pending Approval Requests</h3>
                    <div style="display: flex; flex-direction: column; gap: 0.8rem;">
                        ${this.pendingRequests.map(req => html`
                            <div style="padding: 1rem; background: rgba(56, 189, 248, 0.05); border-left: 3px solid #38bdf8; border-radius: 4px; cursor: pointer;" 
                                 @click="${() => this.selectedRequest = req}">
                                <div style="display: flex; justify-content: space-between; align-items: center;">
                                    <div>
                                        <div style="font-weight: 600; color: #e2e8f0;">${req.username}</div>
                                        <div style="font-size: 0.8rem; color: #94a3b8;">${req.requestType}</div>
                                    </div>
                                    <span style="padding: 0.3rem 0.8rem; background: rgba(250, 204, 21, 0.1); color: #facc15; border-radius: 4px; font-size: 0.7rem; font-weight: 600;">PENDING</span>
                                </div>
                            </div>
                        `)}
                    </div>
                </div>

                ${this.selectedRequest ? html`
                    <div class="card">
                        <h3 style="margin-bottom: 1rem;">Review Request</h3>
                        <div style="background: rgba(255,255,255,0.02); padding: 1rem; border-radius: 8px; margin-bottom: 1rem;">
                            <div style="margin-bottom: 0.8rem;">
                                <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">User</div>
                                <div style="font-weight: 600;">${this.selectedRequest.username}</div>
                            </div>
                            <div style="margin-bottom: 0.8rem;">
                                <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Request Type</div>
                                <div>${this.selectedRequest.requestType}</div>
                            </div>
                            <div style="margin-bottom: 0.8rem;">
                                <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Description</div>
                                <div>${this.selectedRequest.description}</div>
                            </div>
                            <div style="margin-bottom: 1rem;">
                                <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Approval Comment</div>
                                <textarea style="width: 100%; padding: 0.8rem; background: rgba(255,255,255,0.05); border: 1px solid rgba(56,189,248,0.2); border-radius: 6px; color: #e2e8f0; font-family: monospace; font-size: 0.85rem;" 
                                    placeholder="Add approval notes..." 
                                    @input="${(e) => this.approvalComment = e.target.value}"></textarea>
                            </div>
                            <div style="display: flex; gap: 1rem;">
                                <button class="btn-primary" style="flex: 1; background: #4ade80; border: none;" @click="${() => this._approveRequest()}">Approve</button>
                                <button class="btn-primary" style="flex: 1; background: #fb7185; border: none;" @click="${() => this._rejectRequest()}">Reject</button>
                            </div>
                        </div>
                    </div>
                ` : ''}
            </div>

            <div class="card" style="margin-top: 1.5rem;">
                <h3>Global Access Audit Logs</h3>
                <table style="width: 100%; border-collapse: collapse; margin-top: 1rem; font-size: 0.85rem; text-align: left;">
                    <thead>
                        <tr style="color: #64748b; border-bottom: 1px solid rgba(255,255,255,0.05);">
                            <th style="padding: 0.8rem;">Timestamp</th>
                            <th style="padding: 0.8rem;">Subject</th>
                            <th style="padding: 0.8rem;">Action</th>
                            <th style="padding: 0.8rem;">Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${this.pendingRequests.map(req => html`
                            <tr style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                                <td style="padding: 0.8rem;">${req.timestamp}</td>
                                <td style="padding: 0.8rem;">${req.username}</td>
                                <td style="padding: 0.8rem;">${req.requestType}</td>
                                <td style="padding: 0.8rem; color: ${req.status === 'PENDING' ? '#facc15' : '#4ade80'};">${req.status}</td>
                            </tr>
                        `)}
                    </tbody>
                </table>
            </div>
        `;
  }

  _renderResponses() {
    return html`
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem;">
                <div class="card">
                    <h3 style="margin-bottom: 1rem;">Pending Requests to Review</h3>
<<<<<<< HEAD
                    ${this.pendingRequests.filter(r => r.status === 'PENDING').length === 0
        ? html`<p style="color: #94a3b8; text-align: center; padding: 2rem;">No pending requests</p>`
        : html`<div style="display: flex; flex-direction: column; gap: 0.8rem;">
=======
                    ${this.pendingRequests.filter(r => r.status === 'PENDING').length === 0 
                      ? html`<p style="color: #94a3b8; text-align: center; padding: 2rem;">No pending requests</p>`
                      : html`<div style="display: flex; flex-direction: column; gap: 0.8rem;">
>>>>>>> origin/main
                        ${this.pendingRequests.filter(r => r.status === 'PENDING').map(req => html`
                            <div style="padding: 1rem; background: rgba(56, 189, 248, 0.05); border-left: 3px solid #38bdf8; border-radius: 4px; cursor: pointer;" 
                                 @click="${() => this.selectedRequest = req}">
                                <div style="display: flex; justify-content: space-between; align-items: center;">
                                    <div>
                                        <div style="font-weight: 600; color: #e2e8f0;">${req.username}</div>
                                        <div style="font-size: 0.8rem; color: #94a3b8;">${req.requestType}</div>
                                        <div style="font-size: 0.75rem; color: #64748b; margin-top: 0.3rem;">📅 ${req.timestamp}</div>
                                    </div>
                                    <span style="padding: 0.3rem 0.8rem; background: rgba(250, 204, 21, 0.1); color: #facc15; border-radius: 4px; font-size: 0.7rem; font-weight: 600;">PENDING</span>
                                </div>
                            </div>
                        `)}
                    </div>`}
                </div>

                ${this.selectedRequest && this.selectedRequest.status === 'PENDING' ? html`
                    <div class="card">
                        <h3 style="margin-bottom: 1rem;">Respond to Request</h3>
                        <div style="background: rgba(255,255,255,0.02); padding: 1rem; border-radius: 8px; margin-bottom: 1rem;">
                            <div style="margin-bottom: 0.8rem;">
                                <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">From</div>
                                <div style="font-weight: 600; font-size: 1.1rem;">${this.selectedRequest.username}</div>
                            </div>
                            <div style="margin-bottom: 0.8rem;">
                                <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Request Type</div>
                                <div style="padding: 0.8rem; background: rgba(56,189,248,0.1); border-radius: 6px; border-left: 2px solid #38bdf8;">${this.selectedRequest.requestType}</div>
                            </div>
                            <div style="margin-bottom: 0.8rem;">
                                <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Details</div>
                                <div style="padding: 0.8rem; background: rgba(255,255,255,0.02); border-radius: 6px; border: 1px solid rgba(255,255,255,0.1);">${this.selectedRequest.description}</div>
                            </div>
                            <div style="margin-bottom: 1rem;">
                                <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Response Comment</div>
                                <textarea style="width: 100%; padding: 0.8rem; background: rgba(255,255,255,0.05); border: 1px solid rgba(56,189,248,0.2); border-radius: 6px; color: #e2e8f0; font-family: monospace; font-size: 0.85rem; min-height: 80px;" 
                                    placeholder="Add your response/reasoning..." 
                                    @input="${(e) => this.approvalComment = e.target.value}"></textarea>
                            </div>
                            <div style="display: flex; gap: 1rem;">
                                <button class="btn-primary" style="flex: 1; background: #4ade80; border: none; cursor: pointer;" @click="${() => this._approveRequest()}">Approve Request</button>
                                <button class="btn-primary" style="flex: 1; background: #fb7185; border: none; cursor: pointer;" @click="${() => this._rejectRequest()}">Reject Request</button>
                            </div>
                        </div>
                    </div>
                ` : (this.selectedRequest ? html`
                    <div class="card">
                        <h3 style="margin-bottom: 1rem;">Request Details</h3>
                        <div style="padding: 1.5rem; background: ${this.selectedRequest.status === 'APPROVED' ? 'rgba(74, 222, 128, 0.05)' : 'rgba(251, 113, 133, 0.05)'}; border-radius: 8px;">
                            <div style="margin-bottom: 1rem;">
                                <span style="padding: 0.4rem 0.8rem; background: ${this.selectedRequest.status === 'APPROVED' ? 'rgba(74, 222, 128, 0.2); color: #4ade80' : 'rgba(251, 113, 133, 0.2); color: #fb7185'}; border-radius: 4px; font-weight: 600; font-size: 0.8rem;">${this.selectedRequest.status}</span>
                            </div>
                            <div style="color: #94a3b8; margin-bottom: 1rem;">
                                <p><strong>From:</strong> ${this.selectedRequest.username}</p>
                                <p><strong>Type:</strong> ${this.selectedRequest.requestType}</p>
                                <p><strong>Time:</strong> ${this.selectedRequest.timestamp}</p>
                            </div>
                            ${this.selectedRequest.comment ? html`
                                <div style="padding: 0.8rem; background: rgba(255,255,255,0.05); border-left: 2px solid #38bdf8; border-radius: 4px; margin-top: 1rem;">
                                    <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Response</div>
                                    <div>${this.selectedRequest.comment}</div>
                                </div>
                            ` : ''}
                            <button class="btn-ghost" style="margin-top: 1rem; width: 100%;" @click="${() => this.selectedRequest = null}">Back to Requests</button>
                        </div>
                    </div>
                ` : '')}
            </div>

            <div class="card" style="margin-top: 1.5rem;">
                <h3 style="margin-bottom: 1rem;">Response History</h3>
                <table style="width: 100%; border-collapse: collapse; font-size: 0.85rem; text-align: left;">
                    <thead>
                        <tr style="color: #64748b; border-bottom: 1px solid rgba(255,255,255,0.05);">
                            <th style="padding: 0.8rem;">User</th>
                            <th style="padding: 0.8rem;">Request Type</th>
                            <th style="padding: 0.8rem;">Status</th>
                            <th style="padding: 0.8rem;">Time</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${this.pendingRequests.map(req => html`
                            <tr style="border-bottom: 1px solid rgba(255,255,255,0.05);">
                                <td style="padding: 0.8rem;">${req.username}</td>
                                <td style="padding: 0.8rem;">${req.requestType}</td>
                                <td style="padding: 0.8rem; color: ${req.status === 'PENDING' ? '#facc15' : (req.status === 'APPROVED' ? '#4ade80' : '#fb7185')}; font-weight: 600;">${req.status}</td>
                                <td style="padding: 0.8rem; color: #94a3b8;">${req.timestamp}</td>
                            </tr>
                        `)}
                    </tbody>
                </table>
            </div>
        `;
  }

  _renderRequestForm() {
    return html`
            <div class="card" style="max-width: 600px; margin: 0 auto;">
                <span class="status-badge" style="background: rgba(245, 158, 11, 0.2); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3);">
                    Just-In-Time Escalation
                </span>
                <h3 style="margin-top: 1.5rem;">Request Temporary Elevated Privilege</h3>
                <p style="color: #94a3b8; margin-bottom: 2rem; font-size: 0.9rem;">
                    Requests are logged and require an explicit manager signature through the Approval Workflow API.
                </p>
                
                <label style="font-size: 0.8rem; color: #94a3b8;">Resource Zone</label>
                <select id="resource-zone" @change="${(e) => this.requestResource = e.target.value}">
                    <option selected>Infrastucture - Cloud Console [R/W]</option>
                    <option>Database - Main Prod [READ ONLY]</option>
                    <option>CI/CD - Production Deployer</option>
                </select>

                <label style="font-size: 0.8rem; color: #94a3b8;">Window (TTL)</label>
                <select id="window-ttl" @change="${(e) => this.requestWindow = e.target.value}">
                    <option selected>1 Hour (Fast-track Fix)</option>
                    <option>4 Hours (Standard Maintenance)</option>
                    <option>8 Hours (Project Deadline)</option>
                </select>

                <label style="font-size: 0.8rem; color: #94a3b8;">Justification (Audit Log)</label>
                <textarea id="justification" rows="3" placeholder="Explain the security requirement for this JIT request..." @input="${(e) => this.requestJustification = e.target.value}"></textarea>

                <button class="btn-primary" style="margin-top: 1rem;" @click="${() => this._submitRequest()}">
                    Send Request
                </button>
            </div>
        `;
  }

  _renderMyRequests() {
    const myRequests = this.pendingRequests.filter(req => req.username === this.pendingUser);
<<<<<<< HEAD

    return html`
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 1.5rem;">
                ${myRequests.length === 0
        ? html`<div class="card" style="grid-column: 1 / -1; text-align: center; padding: 3rem;">
                      <p style="color: #94a3b8; font-size: 1.1rem;">You haven't submitted any requests yet.</p>
                      <button class="btn-primary" style="margin-top: 1rem;" @click="${() => this.activeView = 'request'}">Submit a Request</button>
                    </div>`
        : myRequests.map(req => html`
                    <div class="card" style="border-left: 4px solid ${req.status === 'PENDING' ? '#facc15' :
            req.status === 'APPROVED' ? '#4ade80' :
              '#fb7185'
          }; cursor: pointer;" @click="${() => this.selectedRequest = req}">
=======
    
    return html`
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 1.5rem;">
                ${myRequests.length === 0 
                  ? html`<div class="card" style="grid-column: 1 / -1; text-align: center; padding: 3rem;">
                      <p style="color: #94a3b8; font-size: 1.1rem;">You haven't submitted any requests yet.</p>
                      <button class="btn-primary" style="margin-top: 1rem;" @click="${() => this.activeView = 'request'}">Submit a Request</button>
                    </div>`
                  : myRequests.map(req => html`
                    <div class="card" style="border-left: 4px solid ${
                      req.status === 'PENDING' ? '#facc15' : 
                      req.status === 'APPROVED' ? '#4ade80' : 
                      '#fb7185'
                    }; cursor: pointer;" @click="${() => this.selectedRequest = req}">
>>>>>>> origin/main
                      <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 1rem;">
                        <div>
                          <h3 style="margin: 0 0 0.3rem 0;">${req.requestType}</h3>
                          <div style="font-size: 0.75rem; color: #94a3b8;">📅 ${req.timestamp}</div>
                        </div>
<<<<<<< HEAD
                        <span style="padding: 0.3rem 0.8rem; background: ${req.status === 'PENDING' ? 'rgba(250, 204, 21, 0.2); color: #facc15' :
            req.status === 'APPROVED' ? 'rgba(74, 222, 128, 0.2); color: #4ade80' :
              'rgba(251, 113, 133, 0.2); color: #fb7185'
          }; border-radius: 4px; font-size: 0.7rem; font-weight: 600;">${req.status}</span>
=======
                        <span style="padding: 0.3rem 0.8rem; background: ${
                          req.status === 'PENDING' ? 'rgba(250, 204, 21, 0.2); color: #facc15' : 
                          req.status === 'APPROVED' ? 'rgba(74, 222, 128, 0.2); color: #4ade80' : 
                          'rgba(251, 113, 133, 0.2); color: #fb7185'
                        }; border-radius: 4px; font-size: 0.7rem; font-weight: 600;">${req.status}</span>
>>>>>>> origin/main
                      </div>
                      <p style="color: #cbd5e1; margin: 0;">${req.description.substring(0, 80)}${req.description.length > 80 ? '...' : ''}</p>
                    </div>
                  `)}
            </div>

            ${this.selectedRequest && this.selectedRequest.username === this.pendingUser ? html`
              <div class="card" style="margin-top: 2rem; max-width: 700px;">
                <h3 style="margin-bottom: 1rem;">Request Details</h3>
                <div style="background: rgba(255,255,255,0.02); padding: 1.5rem; border-radius: 8px;">
                  <div style="margin-bottom: 1rem;">
<<<<<<< HEAD
                    <span style="padding: 0.4rem 0.8rem; background: ${this.selectedRequest.status === 'PENDING' ? 'rgba(250, 204, 21, 0.2); color: #facc15' :
          this.selectedRequest.status === 'APPROVED' ? 'rgba(74, 222, 128, 0.2); color: #4ade80' :
            'rgba(251, 113, 133, 0.2); color: #fb7185'
        }; border-radius: 4px; font-weight: 600; font-size: 0.8rem;">${this.selectedRequest.status}</span>
=======
                    <span style="padding: 0.4rem 0.8rem; background: ${
                      this.selectedRequest.status === 'PENDING' ? 'rgba(250, 204, 21, 0.2); color: #facc15' : 
                      this.selectedRequest.status === 'APPROVED' ? 'rgba(74, 222, 128, 0.2); color: #4ade80' : 
                      'rgba(251, 113, 133, 0.2); color: #fb7185'
                    }; border-radius: 4px; font-weight: 600; font-size: 0.8rem;">${this.selectedRequest.status}</span>
>>>>>>> origin/main
                  </div>
                  
                  <div style="margin-bottom: 1rem;">
                    <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Request Type</div>
                    <div>${this.selectedRequest.requestType}</div>
                  </div>
                  
                  <div style="margin-bottom: 1rem;">
                    <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Your Description</div>
                    <div style="padding: 0.8rem; background: rgba(255,255,255,0.05); border-radius: 6px; border-left: 2px solid #38bdf8;">${this.selectedRequest.description}</div>
                  </div>

                  ${this.selectedRequest.status !== 'PENDING' ? html`
                    <div style="margin-bottom: 1rem;">
                      <div style="font-size: 0.7rem; color: #94a3b8; text-transform: uppercase; margin-bottom: 0.3rem;">Admin Response</div>
<<<<<<< HEAD
                      <div style="padding: 0.8rem; background: ${this.selectedRequest.status === 'APPROVED' ? 'rgba(74, 222, 128, 0.05)' : 'rgba(251, 113, 133, 0.05)'
          }; border-radius: 6px; border-left: 2px solid ${this.selectedRequest.status === 'APPROVED' ? '#4ade80' : '#fb7185'
          };">
=======
                      <div style="padding: 0.8rem; background: ${
                        this.selectedRequest.status === 'APPROVED' ? 'rgba(74, 222, 128, 0.05)' : 'rgba(251, 113, 133, 0.05)'
                      }; border-radius: 6px; border-left: 2px solid ${
                        this.selectedRequest.status === 'APPROVED' ? '#4ade80' : '#fb7185'
                      };">
>>>>>>> origin/main
                        ${this.selectedRequest.comment || 'No additional comments provided.'}
                      </div>
                    </div>
                  ` : html`
                    <div style="padding: 1rem; background: rgba(250, 204, 21, 0.05); border-radius: 6px; border-left: 2px solid #facc15;">
                      <div style="color: #facc15; font-weight: 600; font-size: 0.9rem;">Waiting for admin response...</div>
                      <div style="color: #94a3b8; font-size: 0.85rem; margin-top: 0.3rem;">Your request is under review by the security team.</div>
                    </div>
                  `}
                  
                  <button class="btn-ghost" style="margin-top: 1rem; width: 100%;" @click="${() => this.selectedRequest = null}">Close Details</button>
                </div>
              </div>
            ` : ''}
        `;
  }

  _submitRequest() {
    if (!this.requestJustification.trim()) {
      alert('Please provide justification for your request');
      return;
    }

    const newRequest = {
      id: Math.max(...this.pendingRequests.map(r => r.id), 0) + 1,
      username: this.pendingUser,
      requestType: this.requestResource,
      timestamp: new Date().toLocaleTimeString().slice(0, 5),
      description: this.requestJustification,
      status: "PENDING"
    };

    this.pendingRequests = [...this.pendingRequests, newRequest];
<<<<<<< HEAD

    // Sauvegarder dans localStorage
    localStorage.setItem('secureteam_requests', JSON.stringify(this.pendingRequests));

=======
    
    // Sauvegarder dans localStorage
    localStorage.setItem('secureteam_requests', JSON.stringify(this.pendingRequests));
    
>>>>>>> origin/main
    alert('Your JIT Request has been submitted to the Security Team!');
    this.requestJustification = "";
    this.activeView = 'dashboard';
  }

<<<<<<< HEAD
  async _handleLoginClick() {
=======
  async _toMFAStep() {
>>>>>>> origin/main
    const user = this.shadowRoot.getElementById('user').value;
    const pass = this.shadowRoot.getElementById('pass').value;
    console.log('🔐 Login attempt for user:', user);

    if (user && pass) {
<<<<<<< HEAD
      try {
        const data = await this.authService.login(user, pass);
        console.log("Login Check:", data);

        this.pendingUser = user;
        this.userRole = user === 'admin' ? 'security_admin' : 'external_collaborator';

        if (data.status === 'MFA_SETUP_REQUIRED') {
          await this._setupMfa(user);
        } else {
          // MFA REQUIRED - Show Input
          this.qrImage = null; // No new QR for existing user
          this.loginStep = 2;
          this.requestUpdate();
        }
      } catch (e) {
        alert('Login failed: ' + e.message);
=======
      this.pendingUser = user;
      this.userRole = user === 'admin' ? 'security_admin' : 'external_collaborator';

      // Fetch dynamic QR code from backend
      try {
        console.log('📧 Fetching MFA setup for:', user);
        const resp = await fetch(`/api/auth/mfa/setup?username=${user}`);
        console.log('Response status:', resp.status, 'ok:', resp.ok);
        
        if (!resp.ok) {
          throw new Error(`HTTP ${resp.status}`);
        }
        
        const data = await resp.json();
        console.log('✅ MFA setup data received:', data);
        this.qrImage = data.qrImage;
        console.log('📸 QR Image set, updating loginStep from', this.loginStep, 'to 2...');
        this.loginStep = 2;
        console.log('✅ loginStep is now:', this.loginStep);
        this.requestUpdate();
      } catch (e) {
        console.error("❌ MFA setup failed", e);
        alert("Connection to Security Engine failed. Check if backend is running.");
>>>>>>> origin/main
      }
    } else {
      alert('Invalid credentials');
    }
  }

<<<<<<< HEAD
  async _register() {
    const user = this.shadowRoot.getElementById('reg-user').value;
    const pass = this.shadowRoot.getElementById('reg-pass').value;
    const dept = this.shadowRoot.getElementById('reg-dept').value;

    if (!user || !pass || !dept) {
      alert("All fields are required");
      return;
    }

    try {
      await this.authService.register(user, pass, dept);
      alert("Registration successful! Please Sign In.");
      this.isRegistering = false;
    } catch (e) {
      alert('Registration error: ' + e.message);
    }
  }

  async _setupMfa(user) {
    try {
      console.log('📧 Fetching MFA setup for:', user);
      const resp = await fetch(`/api/auth/mfa/setup?username=${user}`);
      if (!resp.ok) throw new Error("Setup failed");

      const data = await resp.json();
      this.qrImage = data.qrImage;
      this.loginStep = 2;
      this.requestUpdate();
    } catch (e) {
      console.error("❌ MFA setup failed", e);
      alert("MFA Setup failed.");
    }
  }

=======
>>>>>>> origin/main
  async _login() {
    const otp = this.shadowRoot.getElementById('otp').value;
    if (otp.length === 6) {
      try {
<<<<<<< HEAD
        await this.authService.verifyMfa(this.pendingUser, otp);
        this.isAuthenticated = true;
        this.loginStep = 1;
=======
        // CALL REAL BACKEND MFA VERIFY
        const resp = await fetch(`/api/auth/mfa/verify`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username: this.pendingUser, code: otp })
        });

        if (resp.ok) {
          const data = await resp.json();
          localStorage.setItem('secureteam_token', data.token);
          this.isAuthenticated = true;
          this.loginStep = 1;
        } else {
          alert('MFA Verification failed. Incorrect code.');
        }
>>>>>>> origin/main
      } catch (e) {
        alert('MFA Verification error: ' + e.message);
      }
    } else {
      alert('Please enter a valid 6-digit code.');
    }
  }

  _approveRequest() {
    if (!this.selectedRequest) return;
<<<<<<< HEAD

    console.log('Approving request from:', this.selectedRequest.username);

=======
    
    console.log('Approving request from:', this.selectedRequest.username);
    
>>>>>>> origin/main
    const reqIndex = this.pendingRequests.findIndex(r => r.id === this.selectedRequest.id);
    if (reqIndex !== -1) {
      this.pendingRequests[reqIndex].status = 'APPROVED';
      this.pendingRequests[reqIndex].comment = this.approvalComment;
      this.pendingRequests = [...this.pendingRequests];
<<<<<<< HEAD

      // Sauvegarder dans localStorage
      localStorage.setItem('secureteam_requests', JSON.stringify(this.pendingRequests));
    }

=======
      
      // Sauvegarder dans localStorage
      localStorage.setItem('secureteam_requests', JSON.stringify(this.pendingRequests));
    }
    
>>>>>>> origin/main
    alert(`Request from ${this.selectedRequest.username} has been APPROVED!`);
    this.selectedRequest = null;
    this.approvalComment = "";
  }

  _rejectRequest() {
    if (!this.selectedRequest) return;
<<<<<<< HEAD

    console.log('Rejecting request from:', this.selectedRequest.username);

=======
    
    console.log('Rejecting request from:', this.selectedRequest.username);
    
>>>>>>> origin/main
    const reqIndex = this.pendingRequests.findIndex(r => r.id === this.selectedRequest.id);
    if (reqIndex !== -1) {
      this.pendingRequests[reqIndex].status = 'REJECTED';
      this.pendingRequests[reqIndex].comment = this.approvalComment;
      this.pendingRequests = [...this.pendingRequests];
<<<<<<< HEAD

      // Sauvegarder dans localStorage
      localStorage.setItem('secureteam_requests', JSON.stringify(this.pendingRequests));
    }

=======
      
      // Sauvegarder dans localStorage
      localStorage.setItem('secureteam_requests', JSON.stringify(this.pendingRequests));
    }
    
>>>>>>> origin/main
    alert(`Request from ${this.selectedRequest.username} has been REJECTED!`);
    this.selectedRequest = null;
    this.approvalComment = "";
  }

  async _logout() {
    await this.authService.logout();
    this.isAuthenticated = false;
    this.loginStep = 1;
    this.activeView = "dashboard";
    this.qrImage = "";
  }
}

customElements.define('secureteam-app', SecureTeamApp);

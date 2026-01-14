-- Insert test users
INSERT INTO users (username, password, department, mfa_enabled) 
VALUES ('admin', 'password', 'engineering', false);

INSERT INTO users (username, password, department, mfa_enabled) 
VALUES ('user', 'password', 'external_collaborator', false);

-- Assign roles to admin
INSERT INTO user_roles (user_id, roles) 
SELECT id, 'SECURITY_ADMIN' FROM users WHERE username = 'admin';

INSERT INTO user_roles (user_id, roles) 
SELECT id, 'USER' FROM users WHERE username = 'user';

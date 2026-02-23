-- SQL Script to create default admin user
-- Run this in your PostgreSQL database if admin user doesn't exist

-- Insert admin user with BCrypt encoded password (admin123)
-- The password 'admin123' is encoded as: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
INSERT INTO users (username, password, full_name, role, enabled, created_at)
VALUES (
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
    'System Administrator',
    'SUPER_ADMIN',
    TRUE,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO NOTHING;

-- Verify the admin was created
SELECT id, username, full_name, role, enabled, created_at 
FROM users 
WHERE username = 'admin';

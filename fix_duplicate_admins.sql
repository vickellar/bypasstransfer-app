-- Fix duplicate admin users
-- This script will keep only the first admin and delete duplicates

-- First, let's see all users with username 'admin'
SELECT id, username, email, role, created_at FROM users WHERE username = 'admin';

-- Delete password reset tokens for duplicate admins (keep the one with lowest ID)
DELETE FROM password_reset_tokens 
WHERE user_id IN (
    SELECT id FROM users 
    WHERE username = 'admin' 
    AND id != (SELECT MIN(id) FROM users WHERE username = 'admin')
);

-- Delete duplicate admin users (keep the one with lowest ID)
DELETE FROM users 
WHERE username = 'admin' 
AND id != (SELECT MIN(id) FROM users WHERE username = 'admin');

-- Now ensure the remaining admin has the correct password and role
-- Password: admin123 (BCrypt encoded)
UPDATE users 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQzBZN0UfGNEsKYGs5vL7qtXvKxK',
    role = 'SUPER_ADMIN',
    email_verified = true
WHERE username = 'admin';

-- Verify only one admin remains
SELECT id, username, email, role, created_at FROM users WHERE username = 'admin';

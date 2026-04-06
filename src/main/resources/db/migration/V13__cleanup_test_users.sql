-- Clean up dummy users created by PasswordResetIntegrationTest
-- We delete from related tables first to avoid foreign key constraint violations

-- Delete password reset tokens for these users
DELETE FROM password_reset_tokens WHERE user_id IN (
    SELECT id FROM users WHERE username LIKE 'resetuser_%'
);

-- Delete email verification tokens for these users
DELETE FROM email_verification_tokens WHERE user_id IN (
    SELECT id FROM users WHERE username LIKE 'resetuser_%'
);

-- Delete wallets for these users
DELETE FROM wallets WHERE owner_id IN (
    SELECT id FROM users WHERE username LIKE 'resetuser_%'
);

-- Delete the users themselves
DELETE FROM users WHERE username LIKE 'resetuser_%';

-- Fix the foreign key constraint on account table
-- The constraint references app_user but should reference users

-- First, drop the existing foreign key constraint
ALTER TABLE account DROP CONSTRAINT IF EXISTS fkb4a4y8pjba92s54ms9o2t6gs9;

-- Add the correct foreign key constraint pointing to users table
ALTER TABLE account 
ADD CONSTRAINT fk_account_owner 
FOREIGN KEY (owner_id) 
REFERENCES users(id);

-- Now create the missing wallets for all users
-- Create Mukuru wallets for users who don't have one
INSERT INTO account (name, balance, owner_id, transfer_fee, low_balance_threshold, low_balance_alert_sent, currency, locked)
SELECT 'Mukuru', 0.0, u.id, 0.0, 50.0, false, 'USD', false
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM account a WHERE a.owner_id = u.id AND a.name = 'Mukuru'
);

-- Create Econet wallets for users who don't have one
INSERT INTO account (name, balance, owner_id, transfer_fee, low_balance_threshold, low_balance_alert_sent, currency, locked)
SELECT 'Econet', 0.0, u.id, 0.0, 50.0, false, 'USD', false
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM account a WHERE a.owner_id = u.id AND a.name = 'Econet'
);

-- Create Innbucks wallets for users who don't have one
INSERT INTO account (name, balance, owner_id, transfer_fee, low_balance_threshold, low_balance_alert_sent, currency, locked)
SELECT 'Innbucks', 0.0, u.id, 0.0, 50.0, false, 'USD', false
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM account a WHERE a.owner_id = u.id AND a.name = 'Innbucks'
);

-- Verify all users now have 3 wallets each
SELECT u.id, u.username, u.role, COUNT(a.id) as wallet_count
FROM users u
LEFT JOIN account a ON a.owner_id = u.id
GROUP BY u.id, u.username, u.role
ORDER BY u.id;

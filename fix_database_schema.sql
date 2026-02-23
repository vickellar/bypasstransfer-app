-- Comprehensive Database Schema Fix
-- This script fixes the database issues caused by Wallet/Account entity confusion

-- ============================================
-- STEP 1: Check current state
-- ============================================
SELECT 'Current account table structure:' as info;
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'account';

-- ============================================
-- STEP 2: Fix NULL values in existing data
-- ============================================
UPDATE account SET locked = false WHERE locked IS NULL;
UPDATE account SET low_balance_alert_sent = false WHERE low_balance_alert_sent IS NULL;
UPDATE account SET transfer_fee = 0 WHERE transfer_fee IS NULL;
UPDATE account SET low_balance_threshold = 50 WHERE low_balance_threshold IS NULL;

-- ============================================
-- STEP 3: Ensure all users have the required 3 wallets (Mukuru, Econet, Innbucks)
-- ============================================

-- Create Mukuru wallets for users who don't have one
INSERT INTO account (name, balance, owner_id, transfer_fee, low_balance_threshold, low_balance_alert_sent, currency, locked)
SELECT 'Mukuru', 0.0, u.id, 0.015, 50.0, false, 'USD', false
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM account a WHERE a.owner_id = u.id AND a.name = 'Mukuru'
);

-- Create Econet wallets for users who don't have one
INSERT INTO account (name, balance, owner_id, transfer_fee, low_balance_threshold, low_balance_alert_sent, currency, locked)
SELECT 'Econet', 0.0, u.id, 0.033, 50.0, false, 'USD', false
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM account a WHERE a.owner_id = u.id AND a.name = 'Econet'
);

-- Create Innbucks wallets for users who don't have one
INSERT INTO account (name, balance, owner_id, transfer_fee, low_balance_threshold, low_balance_alert_sent, currency, locked)
SELECT 'Innbucks', 0.0, u.id, 0.02, 50.0, false, 'USD', false
FROM users u
WHERE NOT EXISTS (
    SELECT 1 FROM account a WHERE a.owner_id = u.id AND a.name = 'Innbucks'
);

-- ============================================
-- STEP 4: Verify all users now have 3 wallets
-- ============================================
SELECT u.id, u.username, u.role, COUNT(a.id) as wallet_count
FROM users u
LEFT JOIN account a ON a.owner_id = u.id
GROUP BY u.id, u.username, u.role
ORDER BY u.id;

-- ============================================
-- STEP 5: Check for any orphaned transactions
-- ============================================
SELECT 'Orphaned transactions (wallet_id not in account table):' as info;
SELECT t.id, t.wallet_id, t.amount, t.type
FROM public.transaction t
LEFT JOIN account a ON t.wallet_id = a.id
WHERE a.id IS NULL;

-- ============================================
-- STEP 6: Fix orphaned transactions by assigning to user's first wallet
-- (Run this only if you have orphaned transactions)
-- UPDATE public.transaction t
-- SET wallet_id = (SELECT MIN(a.id) FROM account a WHERE a.owner_id = t.created_by)
-- WHERE t.wallet_id NOT IN (SELECT id FROM account);

-- ============================================
-- STEP 7: Summary
-- ============================================
SELECT 'Database fix complete. Summary:' as info;
SELECT 
    (SELECT COUNT(*) FROM users) as total_users,
    (SELECT COUNT(*) FROM account) as total_accounts,
    (SELECT COUNT(*) FROM public.transaction) as total_transactions;

-- Fix wallet table ID sequence issue
-- Run this in PostgreSQL if wallet ID generation fails

-- Check if wallet table exists and fix sequence
DO $$
BEGIN
    -- If the wallet table exists but sequence is not set up
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'wallet') THEN
        -- Create sequence if it doesn't exist
        CREATE SEQUENCE IF NOT EXISTS wallet_id_seq;
        
        -- Set the sequence to the max ID + 1
        SELECT setval('wallet_id_seq', COALESCE((SELECT MAX(id) FROM wallet), 0) + 1, false);
        
        -- Alter the ID column to use the sequence
        ALTER TABLE wallet ALTER COLUMN id SET DEFAULT nextval('wallet_id_seq');
        
        RAISE NOTICE 'Wallet sequence fixed';
    END IF;
END $$;

-- Alternative: If using the 'wallets' table from schema.sql instead of 'wallet'
-- Uncomment below and comment above to use 'wallets' table
/*
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'wallets') THEN
        CREATE SEQUENCE IF NOT EXISTS wallets_id_seq;
        SELECT setval('wallets_id_seq', COALESCE((SELECT MAX(id) FROM wallets), 0) + 1, false);
        ALTER TABLE wallets ALTER COLUMN id SET DEFAULT nextval('wallets_id_seq');
        RAISE NOTICE 'Wallets sequence fixed';
    END IF;
END $$;
*/

-- Fix NULL values in account table that are causing issues
-- The locked field cannot be null

-- First check current state
SELECT id, name, locked, balance FROM account WHERE locked IS NULL;

-- Update all NULL locked values to false
UPDATE account SET locked = false WHERE locked IS NULL;

-- Also ensure other boolean fields have defaults
UPDATE account SET low_balance_alert_sent = false WHERE low_balance_alert_sent IS NULL;

-- Verify the fix
SELECT id, name, locked, balance FROM account;

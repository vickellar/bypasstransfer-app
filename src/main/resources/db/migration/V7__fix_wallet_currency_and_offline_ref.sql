-- Set default currency to USD for wallets with null currency
UPDATE account SET currency = 'USD' WHERE currency IS NULL;

-- Make currency NOT NULL in account table (optional but recommended)
-- ALTER TABLE account ALTER COLUMN currency SET NOT NULL;

-- Add reference_balance to offline_transactions to track wallet balance at time of recording/sync
ALTER TABLE offline_transactions ADD COLUMN IF NOT EXISTS reference_balance DOUBLE PRECISION;

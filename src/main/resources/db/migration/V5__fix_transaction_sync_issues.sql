-- Migration V5: Fix transaction sync issues
-- Resolves: 
-- 1. transaction_type_check violation for 'OUTCOME'
-- 2. value too long for 'notes' column

-- Drop the check constraint on transaction.type if it exists
-- Using DO block for safety and double quotes for the reserved word 'transaction'
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 
        FROM information_schema.table_constraints 
        WHERE table_name = 'transaction' 
        AND constraint_name = 'transaction_type_check'
    ) THEN
        ALTER TABLE "transaction" DROP CONSTRAINT transaction_type_check;
    END IF;
END $$;

-- Increase notes column length in offline_transactions
ALTER TABLE offline_transactions ALTER COLUMN notes TYPE varchar(1000);

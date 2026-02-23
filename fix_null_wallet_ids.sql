-- Fix NULL wallet_id values in transaction table
-- Assign transactions to the first available wallet of the user who created them

-- First, let's see the transactions with NULL wallet_id
SELECT t.id, t.created_by, t.amount, t.type, t.wallet_id
FROM public.transaction t
WHERE t.wallet_id IS NULL;

-- Update transactions with NULL wallet_id to use the user's first wallet
-- This assigns each orphaned transaction to the first wallet of the user who created it
UPDATE public.transaction t
SET wallet_id = (
    SELECT MIN(a.id) 
    FROM account a 
    JOIN users u ON a.owner_id = u.id 
    WHERE u.username = t.created_by
)
WHERE t.wallet_id IS NULL 
AND t.created_by IS NOT NULL;

-- For any remaining transactions where created_by is NULL or no matching user found,
-- assign to the first admin's first wallet as a fallback
UPDATE public.transaction t
SET wallet_id = (
    SELECT MIN(a.id) 
    FROM account a 
    LIMIT 1
)
WHERE t.wallet_id IS NULL;

-- Verify the fix
SELECT t.id, t.wallet_id, t.amount, t.type
FROM public.transaction t
WHERE t.wallet_id IS NULL;

-- Should return 0 rows if all fixed
SELECT COUNT(*) as remaining_null_wallet_ids
FROM public.transaction
WHERE wallet_id IS NULL;

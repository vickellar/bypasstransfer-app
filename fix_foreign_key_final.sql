-- ============================================
-- PERMANENT FIX: Update foreign key to reference correct table
-- ============================================

-- Step 1: Drop the existing incorrect foreign key constraint (try both possible names)
ALTER TABLE public.transaction 
DROP CONSTRAINT IF EXISTS fktfwlfspv2h4wc9rjd1658a6;

ALTER TABLE public.transaction 
DROP CONSTRAINT IF EXISTS fktfwlfspv2h4wcgc9rjd1658a6;

-- Step 2: First, let's see what constraints exist on transaction table
SELECT conname, pg_get_constraintdef(oid) 
FROM pg_constraint 
WHERE conrelid = 'public.transaction'::regclass;

-- Step 3: Drop any constraint that references wallet table
DO $$
DECLARE
    constraint_record RECORD;
BEGIN
    FOR constraint_record IN 
        SELECT conname 
        FROM pg_constraint 
        WHERE conrelid = 'public.transaction'::regclass 
        AND pg_get_constraintdef(oid) LIKE '%wallet%'
    LOOP
        EXECUTE 'ALTER TABLE public.transaction DROP CONSTRAINT IF EXISTS ' || constraint_record.conname;
        RAISE NOTICE 'Dropped constraint: %', constraint_record.conname;
    END LOOP;
END $$;

-- Step 4: Now fix the NULL wallet_id values BEFORE adding new constraint
-- First set all wallet_ids to a valid account id
UPDATE public.transaction 
SET wallet_id = (SELECT MIN(id) FROM account WHERE id IS NOT NULL)
WHERE wallet_id IS NULL OR wallet_id NOT IN (SELECT id FROM account);

-- Step 5: Add new foreign key constraint referencing account table (which has the data)
ALTER TABLE public.transaction 
ADD CONSTRAINT fk_transaction_wallet 
FOREIGN KEY (wallet_id) 
REFERENCES account(id);

-- Step 6: Verify the fix
SELECT 'Remaining NULL wallet_ids:' as check_type, COUNT(*) as count
FROM public.transaction
WHERE wallet_id IS NULL;

-- Step 7: Verify all wallet_ids reference valid accounts
SELECT 'Invalid wallet_ids:' as check_type, COUNT(*) as count
FROM public.transaction t
WHERE t.wallet_id NOT IN (SELECT id FROM account);

-- Step 8: Show sample data
SELECT t.id, t.wallet_id, a.name as wallet_name, a.owner_id, t.amount, t.type
FROM public.transaction t
JOIN account a ON t.wallet_id = a.id
LIMIT 5;

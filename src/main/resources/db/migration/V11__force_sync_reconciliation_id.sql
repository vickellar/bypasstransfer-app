-- V11: Emergency Sequence Resync for daily_reconciliation
-- This is more aggressive than V10 to handle cases where V10 might have failed or been bypassed.

DO $$
DECLARE
    max_id bigint;
BEGIN
    -- 1. Get the current maximum ID in the table
    SELECT MAX(id) INTO max_id FROM daily_reconciliation;
    
    -- 2. If the table is empty, start at 1. If not, start at MAX(id) + 1.
    -- Using setval(sequence, value, true) sets the current value, so nextval will return value + 1.
    -- Using setval(sequence, value, false) sets the next value to be returned.
    
    IF max_id IS NULL THEN
        PERFORM setval('daily_reconciliation_id_seq', 1, false);
    ELSE
        PERFORM setval('daily_reconciliation_id_seq', max_id);
    END IF;
    
    -- 3. Double check the default value for the column
    ALTER TABLE IF EXISTS daily_reconciliation ALTER COLUMN id SET DEFAULT nextval('daily_reconciliation_id_seq');
END $$;

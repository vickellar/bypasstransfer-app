-- V12: Nuclear Sequence Reset for daily_reconciliation
-- This forces the sequence to skip to 1000 to avoid any small overlaps with existing data.

DO $$
BEGIN
    -- 1. Ensure the sequence exists
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = 'public' AND sequencename = 'daily_reconciliation_id_seq') THEN
        CREATE SEQUENCE daily_reconciliation_id_seq;
    END IF;

    -- 2. Jump the sequence to 1000 or (max_id + 10), whichever is higher.
    -- This guarantees we skip past any small blocks of IDs already in the table.
    PERFORM setval('daily_reconciliation_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM daily_reconciliation), 0) + 10, 1000));

    -- 3. Ensure the column uses this sequence
    ALTER TABLE IF EXISTS daily_reconciliation ALTER COLUMN id SET DEFAULT nextval('daily_reconciliation_id_seq');
END $$;

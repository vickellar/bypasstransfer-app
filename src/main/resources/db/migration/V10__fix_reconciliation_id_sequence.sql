-- V10: Fix ID generation for daily_reconciliation
-- Create a sequence for the id column if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_sequences WHERE schemaname = 'public' AND sequencename = 'daily_reconciliation_id_seq') THEN
        CREATE SEQUENCE daily_reconciliation_id_seq;
        -- Set it to the max existing id + 1
        PERFORM setval('daily_reconciliation_id_seq', COALESCE((SELECT MAX(id) FROM daily_reconciliation), 0) + 1, false);
    ELSE
        -- Ensure it's at least as high as the max existing id
        PERFORM setval('daily_reconciliation_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM daily_reconciliation), 0) + 1, nextval('daily_reconciliation_id_seq')), false);
    END IF;
END $$;

-- Set the default for the id column to use this sequence
ALTER TABLE daily_reconciliation ALTER COLUMN id SET DEFAULT nextval('daily_reconciliation_id_seq');

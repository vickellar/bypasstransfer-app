-- V9: Add weekly reconciliation workflow columns to daily_reconciliation
ALTER TABLE daily_reconciliation ADD COLUMN IF NOT EXISTS reconciled_by VARCHAR(255);
ALTER TABLE daily_reconciliation ADD COLUMN IF NOT EXISTS status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE daily_reconciliation ADD COLUMN IF NOT EXISTS notes VARCHAR(1000);
ALTER TABLE daily_reconciliation ADD COLUMN IF NOT EXISTS reviewed_by VARCHAR(255);
ALTER TABLE daily_reconciliation ADD COLUMN IF NOT EXISTS review_notes VARCHAR(1000);
ALTER TABLE daily_reconciliation ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE daily_reconciliation ADD COLUMN IF NOT EXISTS week_number INTEGER;
ALTER TABLE daily_reconciliation ADD COLUMN IF NOT EXISTS recon_year INTEGER;
ALTER TABLE daily_reconciliation ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;

-- Set defaults for existing rows
UPDATE daily_reconciliation SET status = 'APPROVED' WHERE status IS NULL;
UPDATE daily_reconciliation SET created_at = NOW() WHERE created_at IS NULL;

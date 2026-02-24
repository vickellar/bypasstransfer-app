-- Flyway migration V3: add default value for low_balance_alert_sent column

-- Add default value for low_balance_alert_sent column if it exists
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='account' AND column_name='low_balance_alert_sent') THEN
        ALTER TABLE account ALTER COLUMN low_balance_alert_sent SET DEFAULT false;
        UPDATE account SET low_balance_alert_sent = false WHERE low_balance_alert_sent IS NULL;
    END IF;
END$$;
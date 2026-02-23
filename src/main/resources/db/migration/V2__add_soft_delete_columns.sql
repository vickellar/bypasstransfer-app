-- Flyway migration V2: add soft delete columns to users table

-- Add is_active column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='is_active') THEN
        ALTER TABLE users ADD COLUMN is_active boolean DEFAULT true;
    END IF;
END$$;

-- Add deleted_at column if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='deleted_at') THEN
        ALTER TABLE users ADD COLUMN deleted_at timestamp without time zone;
    END IF;
END$$;

-- Update existing users to be active
UPDATE users SET is_active = true WHERE is_active IS NULL;
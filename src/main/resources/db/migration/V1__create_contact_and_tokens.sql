-- Flyway migration V1: create contact and token tables and add user columns

-- Add email_verified and created_at to users if they don't exist
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='email_verified') THEN
        ALTER TABLE users ADD COLUMN email_verified boolean DEFAULT false;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='users' AND column_name='created_at') THEN
        ALTER TABLE users ADD COLUMN created_at timestamp without time zone DEFAULT now();
    END IF;
END$$;

-- Create contact_messages table
CREATE TABLE IF NOT EXISTS contact_messages (
    id bigserial PRIMARY KEY,
    name varchar(255) NOT NULL,
    email varchar(255) NOT NULL,
    message text NOT NULL,
    created_at timestamp without time zone DEFAULT now()
);

-- Create password_reset_tokens table
CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id bigserial PRIMARY KEY,
    token varchar(255) NOT NULL,
    user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    expiry timestamp without time zone
);

-- Create email_verification_tokens table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id bigserial PRIMARY KEY,
    token varchar(255) NOT NULL,
    user_id bigint REFERENCES users(id) ON DELETE CASCADE,
    expiry timestamp without time zone
);

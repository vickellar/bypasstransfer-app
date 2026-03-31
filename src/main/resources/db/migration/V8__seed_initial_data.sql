-- Flyway migration V8: Idempotent initial data seeder
-- Inserts default branches and a minimal set of exchange rates only when missing.

-- Seed branches if branch table exists and rows are not present
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='branch') THEN
        IF NOT EXISTS (SELECT 1 FROM branch WHERE name = 'Zimbabwe Headquarters') THEN
            INSERT INTO branch (name, country, currency, address, contact_email, contact_phone, is_active, created_at, updated_at) VALUES
            ('Zimbabwe Headquarters', 'Zimbabwe', 'USD', 'Harare Main Office', 'zimbabwe@bypasstransers.com', '+263-123-456789', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM branch WHERE name = 'South Africa Branch') THEN
            INSERT INTO branch (name, country, currency, address, contact_email, contact_phone, is_active, created_at, updated_at) VALUES
            ('South Africa Branch', 'South Africa', 'ZAR', 'Johannesburg Office', 'southafrica@bypasstransers.com', '+27-11-123-4567', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;
        IF NOT EXISTS (SELECT 1 FROM branch WHERE name = 'Russia Branch') THEN
            INSERT INTO branch (name, country, currency, address, contact_email, contact_phone, is_active, created_at, updated_at) VALUES
            ('Russia Branch', 'Russia', 'RUB', 'Moscow Office', 'russia@bypasstransers.com', '+7-495-123-4567', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
        END IF;
    END IF;
END$$;

-- Seed a small set of exchange rates if exchange_rate table exists and USD->USD is missing
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='exchange_rate') THEN
        IF NOT EXISTS (SELECT 1 FROM exchange_rate WHERE from_currency='USD' AND to_currency='USD') THEN
            INSERT INTO exchange_rate (from_currency, to_currency, rate, source, last_updated) VALUES
            ('USD', 'USD', 1.0, 'INITIAL', CURRENT_TIMESTAMP),
            ('USD', 'ZAR', 18.45, 'INITIAL', CURRENT_TIMESTAMP),
            ('USD', 'RUB', 92.50, 'INITIAL', CURRENT_TIMESTAMP);
        END IF;
    END IF;
END$$;

-- Assign existing users to Zimbabwe Headquarters if users table exists and branch was created
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='users') AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='branch') THEN
        UPDATE users SET branch_id = (SELECT id FROM branch WHERE name = 'Zimbabwe Headquarters' LIMIT 1)
        WHERE branch_id IS NULL AND (SELECT COUNT(*) FROM branch WHERE name = 'Zimbabwe Headquarters') > 0;
    END IF;
END$$;

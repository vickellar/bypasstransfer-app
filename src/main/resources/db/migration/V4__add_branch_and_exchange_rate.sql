-- Migration script to add branch management and exchange rate tables
-- Version 4 - Multi-currency and Branch Support

-- Create branch table
CREATE TABLE branch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    country VARCHAR(100) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    address VARCHAR(500),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create exchange_rate table
CREATE TABLE exchange_rate (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_currency VARCHAR(10) NOT NULL,
    to_currency VARCHAR(10) NOT NULL,
    rate DOUBLE PRECISION NOT NULL,
    source VARCHAR(50) NOT NULL,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_currency_pair UNIQUE (from_currency, to_currency)
);

-- Add branch_id and base_currency to users table
ALTER TABLE users 
ADD COLUMN branch_id BIGINT,
ADD COLUMN base_currency VARCHAR(10);

-- Add foreign key constraint for users.branch_id
ALTER TABLE users 
ADD CONSTRAINT fk_users_branch 
FOREIGN KEY (branch_id) REFERENCES branch(id) ON DELETE SET NULL;

-- Add branch_id to account (wallet) table
ALTER TABLE account 
ADD COLUMN branch_id BIGINT;

-- Add foreign key constraint for account.branch_id
ALTER TABLE account 
ADD CONSTRAINT fk_account_branch 
FOREIGN KEY (branch_id) REFERENCES branch(id) ON DELETE SET NULL;

-- Insert default branches
INSERT INTO branch (name, country, currency, address, contact_email, contact_phone, is_active) VALUES
('Zimbabwe Headquarters', 'Zimbabwe', 'USD', 'Harare Main Office', 'zimbabwe@bypasstransers.com', '+263-123-456789', TRUE),
('South Africa Branch', 'South Africa', 'ZAR', 'Johannesburg Office', 'southafrica@bypasstransers.com', '+27-11-123-4567', TRUE),
('Russia Branch', 'Russia', 'RUB', 'Moscow Office', 'russia@bypasstransers.com', '+7-495-123-4567', TRUE);

-- Insert initial exchange rates (example rates - will be updated by API)
-- USD as base
INSERT INTO exchange_rate (from_currency, to_currency, rate, source, last_updated) VALUES
('USD', 'USD', 1.0, 'INITIAL', CURRENT_TIMESTAMP),
('USD', 'ZAR', 18.45, 'INITIAL', CURRENT_TIMESTAMP),
('USD', 'RUB', 92.50, 'INITIAL', CURRENT_TIMESTAMP),
('USD', 'EUR', 0.92, 'INITIAL', CURRENT_TIMESTAMP),
('USD', 'GBP', 0.79, 'INITIAL', CURRENT_TIMESTAMP),
('USD', 'ZWL', 3750.00, 'INITIAL', CURRENT_TIMESTAMP),
('USD', 'NGN', 1550.00, 'INITIAL', CURRENT_TIMESTAMP),
('USD', 'KES', 129.50, 'INITIAL', CURRENT_TIMESTAMP);

-- ZAR cross rates
INSERT INTO exchange_rate (from_currency, to_currency, rate, source, last_updated) VALUES
('ZAR', 'USD', 0.0542, 'INITIAL', CURRENT_TIMESTAMP),
('ZAR', 'RUB', 5.01, 'INITIAL', CURRENT_TIMESTAMP),
('ZAR', 'EUR', 0.050, 'INITIAL', CURRENT_TIMESTAMP),
('ZAR', 'GBP', 0.043, 'INITIAL', CURRENT_TIMESTAMP),
('ZAR', 'ZWL', 203.26, 'INITIAL', CURRENT_TIMESTAMP);

-- RUB cross rates
INSERT INTO exchange_rate (from_currency, to_currency, rate, source, last_updated) VALUES
('RUB', 'USD', 0.0108, 'INITIAL', CURRENT_TIMESTAMP),
('RUB', 'ZAR', 0.1996, 'INITIAL', CURRENT_TIMESTAMP),
('RUB', 'EUR', 0.00998, 'INITIAL', CURRENT_TIMESTAMP),
('RUB', 'GBP', 0.00858, 'INITIAL', CURRENT_TIMESTAMP),
('RUB', 'ZWL', 40.54, 'INITIAL', CURRENT_TIMESTAMP);

-- EUR cross rates
INSERT INTO exchange_rate (from_currency, to_currency, rate, source, last_updated) VALUES
('EUR', 'USD', 1.087, 'INITIAL', CURRENT_TIMESTAMP),
('EUR', 'ZAR', 20.05, 'INITIAL', CURRENT_TIMESTAMP),
('EUR', 'RUB', 100.54, 'INITIAL', CURRENT_TIMESTAMP),
('EUR', 'GBP', 0.86, 'INITIAL', CURRENT_TIMESTAMP),
('EUR', 'ZWL', 4076.09, 'INITIAL', CURRENT_TIMESTAMP);

-- GBP cross rates
INSERT INTO exchange_rate (from_currency, to_currency, rate, source, last_updated) VALUES
('GBP', 'USD', 1.267, 'INITIAL', CURRENT_TIMESTAMP),
('GBP', 'ZAR', 23.35, 'INITIAL', CURRENT_TIMESTAMP),
('GBP', 'RUB', 116.88, 'INITIAL', CURRENT_TIMESTAMP),
('GBP', 'EUR', 1.163, 'INITIAL', CURRENT_TIMESTAMP),
('GBP', 'ZWL', 4739.53, 'INITIAL', CURRENT_TIMESTAMP);

-- Note: Existing users and wallets will need to be assigned to default branch (Zimbabwe - ID=1)
-- This will be done via application startup or manual script

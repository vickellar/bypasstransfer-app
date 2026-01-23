--Postresql database tables
--Schema Name: bypass_records

-- ===============================
-- 1. CREATE SCHEMA
-- ===============================
CREATE SCHEMA IF NOT EXISTS bypass;
SET search_path TO bypass;

-- ===============================
-- 2. USERS TABLE
-- ===============================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    role VARCHAR(30) NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===============================
-- 3. WALLETS TABLE
-- ===============================
CREATE TABLE wallets (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_type VARCHAR(30) NOT NULL,
    currency VARCHAR(10) NOT NULL,
    balance DECIMAL(18,2) DEFAULT 0,
    locked BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallet_user
        FOREIGN KEY (user_id)
        REFERENCES users(id)
);

-- ===============================
-- 4. TRANSACTIONS TABLE
-- ===============================
CREATE TABLE transactions (
    id BIGSERIAL PRIMARY KEY,
    wallet_id BIGINT NOT NULL,
    direction VARCHAR(20) NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    charge DECIMAL(18,2) DEFAULT 0,
    net_amount DECIMAL(18,2) NOT NULL,
    reference VARCHAR(100),
    description TEXT,
    transaction_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    sync_status VARCHAR(20) DEFAULT 'SYNC_PENDING',
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transaction_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES wallets(id),

    CONSTRAINT fk_transaction_user
        FOREIGN KEY (created_by)
        REFERENCES users(id)
);

-- ===============================
-- 5. TRANSACTION PROOFS
-- ===============================
CREATE TABLE transaction_proofs (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    file_type VARCHAR(50),
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_proof_transaction
        FOREIGN KEY (transaction_id)
        REFERENCES transactions(id)
        ON DELETE CASCADE
);

-- ===============================
-- 6. TRANSFERS TABLE
-- ===============================
CREATE TABLE transfers (
    id BIGSERIAL PRIMARY KEY,
    from_wallet_id BIGINT NOT NULL,
    to_wallet_id BIGINT NOT NULL,
    amount DECIMAL(18,2) NOT NULL,
    charge DECIMAL(18,2) DEFAULT 0,
    net_amount DECIMAL(18,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transfer_from_wallet
        FOREIGN KEY (from_wallet_id)
        REFERENCES wallets(id),

    CONSTRAINT fk_transfer_to_wallet
        FOREIGN KEY (to_wallet_id)
        REFERENCES wallets(id)
);

-- ===============================
-- 7. EXCHANGE RATES
-- ===============================
CREATE TABLE exchange_rates (
    id BIGSERIAL PRIMARY KEY,
    from_currency VARCHAR(10) NOT NULL,
    to_currency VARCHAR(10) NOT NULL,
    rate DECIMAL(18,6) NOT NULL,
    source VARCHAR(50),
    effective_date DATE NOT NULL
);

-- ===============================
-- 8. AUDIT LOGS
-- ===============================
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(50) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(30) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    performed_by BIGINT,
    performed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_user
        FOREIGN KEY (performed_by)
        REFERENCES users(id)
);

-- ===============================
-- 9. SYNC LOGS
-- ===============================
CREATE TABLE sync_logs (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(100),
    last_sync_time TIMESTAMP,
    status VARCHAR(30),
    message TEXT
);

-- ===============================
-- 10. BACKUPS
-- ===============================
CREATE TABLE backups (
    id BIGSERIAL PRIMARY KEY,
    backup_type VARCHAR(30) NOT NULL,
    file_path VARCHAR(255) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_backup_user
        FOREIGN KEY (created_by)
        REFERENCES users(id)
);

-- ===============================
-- 11. INDEXES (PERFORMANCE)
-- ===============================
CREATE INDEX idx_transactions_wallet
    ON transactions(wallet_id);

CREATE INDEX idx_transactions_date
    ON transactions(transaction_date);

CREATE INDEX idx_transactions_sync
    ON transactions(sync_status);

CREATE INDEX idx_wallet_user
    ON wallets(user_id);
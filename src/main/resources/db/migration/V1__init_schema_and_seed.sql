-- V1__init_schema_and_seed.sql
-- Combined migration: schema + seed data + corrected password hashes
-- Credentials:
--   admin@finance.com    / Admin@1234
--   analyst@finance.com  / Analyst@1234
--   viewer@finance.com   / Viewer@1234

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users
CREATE TABLE users (
                       id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
                       name          VARCHAR(100) NOT NULL,
                       email         VARCHAR(255) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       role          VARCHAR(20)  NOT NULL CHECK (role IN ('VIEWER', 'ANALYST', 'ADMIN')),
                       status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
                       created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
                       updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_role   ON users (role);
CREATE INDEX idx_users_status ON users (status);

-- Financial records
CREATE TABLE financial_records (
                                   id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
                                   user_id     UUID           NOT NULL REFERENCES users(id),
                                   amount      NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
                                   type        VARCHAR(10)    NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
                                   category    VARCHAR(100)   NOT NULL,
                                   record_date DATE           NOT NULL,
                                   notes       VARCHAR(1000),
                                   is_deleted  BOOLEAN        NOT NULL DEFAULT FALSE,
                                   created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
                                   updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_record_user_id    ON financial_records (user_id);
CREATE INDEX idx_record_type       ON financial_records (type);
CREATE INDEX idx_record_date       ON financial_records (record_date DESC);
CREATE INDEX idx_record_category   ON financial_records (LOWER(category));
CREATE INDEX idx_record_is_deleted ON financial_records (is_deleted);

CREATE INDEX idx_record_active_type_date
    ON financial_records (is_deleted, type, record_date DESC)
    WHERE is_deleted = FALSE;

-- Audit log
CREATE TABLE audit_log (
                           id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
                           actor_id    UUID        REFERENCES users(id) ON DELETE SET NULL,
                           actor_email VARCHAR(255),
                           action      VARCHAR(100) NOT NULL,
                           entity_type VARCHAR(50)  NOT NULL,
                           entity_id   VARCHAR(255),
                           details     TEXT,
                           created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_actor_id   ON audit_log (actor_id);
CREATE INDEX idx_audit_created_at ON audit_log (created_at DESC);

-- Auto-update updated_at via trigger
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_records_updated_at
    BEFORE UPDATE ON financial_records
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- Seed users
INSERT INTO users (id, name, email, password_hash, role, status) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'Admin',   'admin@finance.com',   '$2a$12$Qrl/G5Iux40tpKVYXSeNx.EvG.YCXfD/j4Uk2mRKVANp.8wsXqMjy', 'ADMIN',   'ACTIVE'),
    ('a0000000-0000-0000-0000-000000000002', 'Analyst', 'analyst@finance.com', '$2a$12$/qJ6L8zUcSSnGtBlwMXk9eWXifwjPeddP9vIROoiZDnFhzk1zDRXW', 'ANALYST', 'ACTIVE'),
    ('a0000000-0000-0000-0000-000000000003', 'Viewer',  'viewer@finance.com',  '$2a$12$LjkwX.SKLVMI2smQwR2rveLH3gcTC3gkV3A8mrno9VyIvmrzLQOXa', 'VIEWER',  'ACTIVE');

-- Sample financial records
INSERT INTO financial_records (user_id, amount, type, category, record_date, notes) VALUES
-- Income
('a0000000-0000-0000-0000-000000000001', 85000.00, 'INCOME', 'Salary',      '2026-01-01', 'January salary'),
('a0000000-0000-0000-0000-000000000001', 85000.00, 'INCOME', 'Salary',      '2026-02-01', 'February salary'),
('a0000000-0000-0000-0000-000000000001', 85000.00, 'INCOME', 'Salary',      '2026-03-01', 'March salary'),
('a0000000-0000-0000-0000-000000000001', 12000.00, 'INCOME', 'Freelance',   '2026-01-15', 'Web project'),
('a0000000-0000-0000-0000-000000000001',  3500.00, 'INCOME', 'Investments', '2026-02-20', 'Dividend payout'),
('a0000000-0000-0000-0000-000000000001',  2000.00, 'INCOME', 'Freelance',   '2026-03-10', 'Design work'),
-- Expenses
('a0000000-0000-0000-0000-000000000001', 18000.00, 'EXPENSE', 'Rent',        '2026-01-05', 'Monthly rent'),
('a0000000-0000-0000-0000-000000000001', 18000.00, 'EXPENSE', 'Rent',        '2026-02-05', 'Monthly rent'),
('a0000000-0000-0000-0000-000000000001', 18000.00, 'EXPENSE', 'Rent',        '2026-03-05', 'Monthly rent'),
('a0000000-0000-0000-0000-000000000001',  4500.00, 'EXPENSE', 'Groceries',   '2026-01-12', 'Weekly groceries'),
('a0000000-0000-0000-0000-000000000001',  4200.00, 'EXPENSE', 'Groceries',   '2026-02-14', 'Weekly groceries'),
('a0000000-0000-0000-0000-000000000001',  1200.00, 'EXPENSE', 'Utilities',   '2026-01-20', 'Electricity and water'),
('a0000000-0000-0000-0000-000000000001',  1100.00, 'EXPENSE', 'Utilities',   '2026-02-20', 'Electricity and water'),
('a0000000-0000-0000-0000-000000000001',  3800.00, 'EXPENSE', 'Transport',   '2026-01-25', 'Fuel and maintenance'),
('a0000000-0000-0000-0000-000000000001',  2200.00, 'EXPENSE', 'Healthcare',  '2026-02-08', 'Doctor and medicine'),
('a0000000-0000-0000-0000-000000000001',  6500.00, 'EXPENSE', 'Travel',      '2026-03-18', 'Business trip'),
('a0000000-0000-0000-0000-000000000001',   800.00, 'EXPENSE', 'Subscriptions','2026-01-01','Annual software subs');

-- Seed data for the three Phase 2 personas. Birthdates use June 1 so age math
-- (Period.between(dateOfBirth, now)) stays correct for a long time after seeding,
-- not just on the day this was written (Phase 4 seed plan).

INSERT INTO investor (id, first_name, last_name, date_of_birth, email) VALUES
    (1, 'Thabo', 'Nkosi', '1981-06-01', 'thabo.nkosi@example.com'),
    (2, 'Grace', 'van der Merwe', '1958-06-01', 'grace.vandermerwe@example.com'),
    (3, 'Sipho', 'Dlamini', '1966-06-01', 'sipho.dlamini@example.com');

INSERT INTO portfolio (id, investor_id, portfolio_number, created_at) VALUES
    (1, 1, 'PF-0001', '2025-01-10 09:00:00'),
    (2, 2, 'PF-0002', '2025-01-10 09:00:00'),
    (3, 3, 'PF-0003', '2025-01-10 09:00:00');

INSERT INTO product (id, portfolio_id, name, product_type, balance, created_at, updated_at) VALUES
    (1, 1, 'Unit Trust', 'UNIT_TRUST', 50000.00, '2025-01-10 09:00:00', '2026-09-15 10:00:00'),
    (2, 1, 'Money Market', 'MONEY_MARKET', 20000.00, '2025-01-10 09:00:00', '2025-01-10 09:00:00'),
    (3, 2, 'Retirement Annuity', 'RETIREMENT_ANNUITY', 800000.00, '2025-01-10 09:00:00', '2026-09-16 11:00:00'),
    (4, 3, 'Retirement Annuity', 'RETIREMENT_ANNUITY', 300000.00, '2025-01-10 09:00:00', '2025-01-10 09:00:00');

INSERT INTO withdrawal_notice (product_id, investor_id, type, amount, status, rejection_reason, balance_after, requested_at) VALUES
    (1, 1, 'STANDARD', 5000.00, 'APPROVED', NULL, 50000.00, '2026-09-15 10:00:00'),
    (3, 2, 'RETIREMENT', 100000.00, 'APPROVED', NULL, 800000.00, '2026-09-16 11:00:00'),
    (4, 3, 'RETIREMENT', 50000.00, 'REJECTED', 'Retirement withdrawals require age > 65 (investor is 60)', NULL, '2026-09-17 14:30:00');

ALTER TABLE investor ALTER COLUMN id RESTART WITH 4;
ALTER TABLE portfolio ALTER COLUMN id RESTART WITH 4;
ALTER TABLE product ALTER COLUMN id RESTART WITH 5;

-- Seed data for the three demo investors. Birthdates use June 1 so age math
-- (Period.between(dateOfBirth, now)) stays correct for a long time after seeding,
-- not just on the day this was written.
--
-- Each investor holds a mix of standard and retirement-category products (all five
-- ProductType values are represented at least once) and a spread of withdrawal notices
-- across dates/types/statuses/rejection reasons, so the history filters and CSV export
-- have something non-trivial to filter.

INSERT INTO investor (id, first_name, last_name, date_of_birth, email) VALUES
    (1, 'Thabo', 'Nkosi', '1981-06-01', 'thabo.nkosi@example.com'),
    (2, 'Grace', 'van der Merwe', '1958-06-01', 'grace.vandermerwe@example.com'),
    (3, 'Sipho', 'Dlamini', '1966-06-01', 'sipho.dlamini@example.com');

INSERT INTO portfolio (id, investor_id, portfolio_number, created_at) VALUES
    (1, 1, 'PF-0001', '2025-01-10 09:00:00'),
    (2, 2, 'PF-0002', '2025-01-10 09:00:00'),
    (3, 3, 'PF-0003', '2025-01-10 09:00:00');

-- Balances are each product's *current* balance, i.e. already net of its approved
-- withdrawals below; updated_at matches the timestamp of the latest approved withdrawal
-- against that product (or created_at if it's never been debited).
INSERT INTO product (id, portfolio_id, name, product_type, balance, created_at, updated_at) VALUES
    (1, 1, 'Unit Trust', 'UNIT_TRUST', 45000.00, '2025-01-10 09:00:00', '2026-08-10 10:00:00'),
    (2, 1, 'Money Market', 'MONEY_MARKET', 17000.00, '2025-01-10 09:00:00', '2026-07-05 09:30:00'),
    (3, 1, 'Tax-Free Savings Account', 'TAX_FREE_SAVINGS', 15000.00, '2025-01-10 09:00:00', '2025-01-10 09:00:00'),
    (4, 2, 'Retirement Annuity', 'RETIREMENT_ANNUITY', 650000.00, '2025-01-10 09:00:00', '2026-09-01 09:00:00'),
    (5, 2, 'Preservation Fund', 'PRESERVATION_FUND', 230000.00, '2025-01-10 09:00:00', '2026-08-20 13:00:00'),
    (6, 2, 'Money Market', 'MONEY_MARKET', 50000.00, '2025-01-10 09:00:00', '2026-09-12 10:15:00'),
    (7, 3, 'Retirement Annuity', 'RETIREMENT_ANNUITY', 300000.00, '2025-01-10 09:00:00', '2025-01-10 09:00:00'),
    (8, 3, 'Unit Trust', 'UNIT_TRUST', 32000.00, '2025-01-10 09:00:00', '2026-09-18 11:00:00');

INSERT INTO withdrawal_notice (product_id, investor_id, type, amount, status, rejection_reason, balance_after, requested_at) VALUES
    -- Thabo (45, standard-only) — one approved withdrawal per touched product, plus a 90%-rule rejection
    (1, 1, 'STANDARD', 5000.00, 'APPROVED', NULL, 45000.00, '2026-08-10 10:00:00'),
    (2, 1, 'STANDARD', 3000.00, 'APPROVED', NULL, 17000.00, '2026-07-05 09:30:00'),
    (3, 1, 'STANDARD', 12000.00, 'REJECTED', 'Withdrawal amount exceeds 90% of available balance', NULL, '2026-09-10 14:00:00'),

    -- Grace (68, retirement-eligible) — two retirement withdrawals, one standard, one 90%-rule rejection
    (4, 2, 'RETIREMENT', 100000.00, 'APPROVED', NULL, 700000.00, '2026-06-16 11:00:00'),
    (4, 2, 'RETIREMENT', 50000.00, 'APPROVED', NULL, 650000.00, '2026-09-01 09:00:00'),
    (5, 2, 'RETIREMENT', 215000.00, 'REJECTED', 'Withdrawal amount exceeds 90% of available balance', NULL, '2026-09-02 08:45:00'),
    (6, 2, 'STANDARD', 10000.00, 'APPROVED', NULL, 50000.00, '2026-09-12 10:15:00'),

    -- Sipho (60, blocked from retirement) — repeated age rejections, plus a standard product he can use freely
    (7, 3, 'RETIREMENT', 50000.00, 'REJECTED', 'Retirement withdrawals require age > 65 (investor is 60)', NULL, '2026-09-17 14:30:00'),
    (7, 3, 'RETIREMENT', 20000.00, 'REJECTED', 'Retirement withdrawals require age > 65 (investor is 60)', NULL, '2026-09-20 09:00:00'),
    (8, 3, 'STANDARD', 8000.00, 'APPROVED', NULL, 32000.00, '2026-09-18 11:00:00'),
    (8, 3, 'STANDARD', 40000.00, 'REJECTED', 'Withdrawal amount exceeds available balance', NULL, '2026-09-19 12:00:00');

ALTER TABLE investor ALTER COLUMN id RESTART WITH 4;
ALTER TABLE portfolio ALTER COLUMN id RESTART WITH 4;
ALTER TABLE product ALTER COLUMN id RESTART WITH 9;

# Phase 4: Database Design — Enviro365 Withdrawal Notice System

Builds on the domain assumptions in [phase2-requirements.md](phase2-requirements.md) and the module boundaries in [phase3-system-design.md](phase3-system-design.md).

## Entities & Relationships

- **Investor** — a person; stores `dateOfBirth` rather than a static `age` so the age > 65 rule is always computed correctly, not frozen at seed time.
- **Portfolio** — one per investor (1:1); a thin container, kept as its own table (rather than folded into Investor) because the brief explicitly frames "retrieve investor portfolio" as its own concept and it's a natural place to hang a portfolio-level reference number.
- **Product** — many per portfolio (1:*); each holds the `balance` that withdrawals are validated and deducted against, plus a permanent `productType` (see below).
- **WithdrawalNotice** — many per product (1:*); records every withdrawal *attempt*, approved or rejected, so the history table can show rejection reasons (per US4/US6 in Phase 2). Also carries a denormalized `investor_id` (see rationale below).

**Denormalization note:** `withdrawal_notice.investor_id` duplicates data reachable via `product → portfolio → investor`. This is a deliberate tradeoff: the dominant query pattern is "this investor's withdrawal history / CSV export," and a direct FK avoids a 3-table join for every request. It's set once at creation and never updated, so there's no sync-drift risk.

**Product type rework (Epic I, added after Epic H):** the original model gave `Product` only a free-text `name` and left "is this a retirement product?" as a field on the *withdrawal request* (`WithdrawalRequest.type`), chosen by the client on every submission. That let a request declare `STANDARD` against a retirement account and skip the age > 65 rule — the rule depended on what the caller claimed, not on what the product actually was. Fixed by adding `product.product_type`, a fixed enum (`UNIT_TRUST`, `MONEY_MARKET`, `TAX_FREE_SAVINGS`, `RETIREMENT_ANNUITY`, `PRESERVATION_FUND` — not free text), set once when the product is created and never changed by a withdrawal. Each `ProductType` maps to exactly one category, `STANDARD` or `RETIREMENT` (`RETIREMENT_ANNUITY` and `PRESERVATION_FUND` are `RETIREMENT`; the rest are `STANDARD`). `WithdrawalNotice.type` still exists and still records `STANDARD`/`RETIREMENT` for history/CSV display, but it's now derived server-side from `product.product_type`'s category at submission time — `WithdrawalRequest` no longer has a `type` field at all, so there's nothing for a client to lie about. `name` is kept as a separate free-text display label (e.g. "My Retirement Annuity") — it's not what drives the business rule.

## ERD

```
┌───────────────────┐
│     Investor      │
├───────────────────┤
│ PK id             │
│    firstName      │
│    lastName       │
│    dateOfBirth    │
│    email (unique) │
└─────────┬─────────┘
          │  1 : 1  (one investor → exactly one portfolio)
          ▼
┌──────────────────────────────┐
│          Portfolio           │
├──────────────────────────────┤
│ PK id                        │
│ FK investor_id (unique)      │
│    portfolioNumber (unique)  │
│    createdAt                 │
└─────────┬────────────────────┘
          │  1 : *  (one portfolio → many products)
          ▼
┌───────────────────┐
│      Product      │
├───────────────────┤
│ PK id             │
│ FK portfolio_id   │
│    name           │
│    productType    │
│    balance (>= 0) │
│    createdAt      │
│    updatedAt      │
└─────────┬─────────┘
          │  1 : *  (one product → many withdrawal notices)
          ▼
┌──────────────────────────────────┐
│         WithdrawalNotice         │
├──────────────────────────────────┤
│ PK id                            │
│ FK product_id                    │
│ FK investor_id (denormalized)    │
│    type (STANDARD | RETIREMENT)  │  ← derived server-side from product.productType, not client input
│    amount (> 0)                  │
│    status (APPROVED | REJECTED)  │
│    rejectionReason (nullable)    │
│    balanceAfter (nullable)       │
│    requestedAt                   │
└──────────────────────────────────┘
```

## Database Schema (H2-compatible DDL)

```sql
CREATE TABLE investor (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    first_name     VARCHAR(100) NOT NULL,
    last_name      VARCHAR(100) NOT NULL,
    date_of_birth  DATE NOT NULL,
    email          VARCHAR(255) NOT NULL,
    CONSTRAINT uq_investor_email UNIQUE (email)
);

CREATE TABLE portfolio (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    investor_id       BIGINT NOT NULL,
    portfolio_number  VARCHAR(50) NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_portfolio_investor UNIQUE (investor_id),
    CONSTRAINT uq_portfolio_number UNIQUE (portfolio_number),
    CONSTRAINT fk_portfolio_investor FOREIGN KEY (investor_id) REFERENCES investor(id)
);

CREATE TABLE product (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_id  BIGINT NOT NULL,
    name          VARCHAR(100) NOT NULL,
    product_type  VARCHAR(30) NOT NULL,
    balance       DECIMAL(19,2) NOT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_product_balance_non_negative CHECK (balance >= 0),
    CONSTRAINT ck_product_type CHECK (product_type IN
        ('UNIT_TRUST', 'MONEY_MARKET', 'TAX_FREE_SAVINGS', 'RETIREMENT_ANNUITY', 'PRESERVATION_FUND')),
    CONSTRAINT fk_product_portfolio FOREIGN KEY (portfolio_id) REFERENCES portfolio(id)
);

CREATE TABLE withdrawal_notice (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id        BIGINT NOT NULL,
    investor_id       BIGINT NOT NULL,
    type              VARCHAR(20) NOT NULL,
    amount            DECIMAL(19,2) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    rejection_reason  VARCHAR(255),
    balance_after     DECIMAL(19,2),
    requested_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_withdrawal_type CHECK (type IN ('STANDARD', 'RETIREMENT')),
    CONSTRAINT ck_withdrawal_status CHECK (status IN ('APPROVED', 'REJECTED')),
    CONSTRAINT ck_withdrawal_amount_positive CHECK (amount > 0),
    CONSTRAINT fk_withdrawal_product FOREIGN KEY (product_id) REFERENCES product(id),
    CONSTRAINT fk_withdrawal_investor FOREIGN KEY (investor_id) REFERENCES investor(id)
);
```

## Index Strategy

| Index | Columns | Reason |
|---|---|---|
| (implicit, via `UNIQUE`) | `portfolio.investor_id` | Enforces the 1:1 rule and speeds up "find this investor's portfolio" |
| (implicit, via `UNIQUE`) | `portfolio.portfolio_number` | Human-readable lookup, no duplicates |
| (implicit, via `UNIQUE`) | `investor.email` | No duplicate accounts |
| `idx_product_portfolio` | `product.portfolio_id` | Every portfolio-view request joins products via this FK |
| `idx_withdrawal_investor_requested` | `withdrawal_notice(investor_id, requested_at)` | Composite index for the dominant query: "this investor's withdrawals, filtered/sorted by date" (history table + CSV export, FR3/FR4) |

```sql
CREATE INDEX idx_product_portfolio ON product (portfolio_id);
CREATE INDEX idx_withdrawal_investor_requested ON withdrawal_notice (investor_id, requested_at);
```

No index on `type` alone — with the small row counts this system will ever hold (single-investor demo data), the composite index above already narrows to a small row set that a full scan of `type` on those rows costs nothing to filter further. Not worth the extra write overhead of another index for the assessment's scale.

## Constraints Summary

- **Referential integrity**: every FK (`portfolio.investor_id`, `product.portfolio_id`, `withdrawal_notice.product_id`, `withdrawal_notice.investor_id`) is enforced at the DB level, not just in application code.
- **Business invariants pushed into the schema where possible**: `product.balance >= 0`, `withdrawal_notice.amount > 0`, `type`/`status` restricted to known values via `CHECK`. This is a second line of defense — the *decision* logic (age > 65, ≤ balance, ≤ 90%) still lives in the service layer (FR5) because it needs cross-row context (investor's age, product's current balance) that a single-table `CHECK` constraint can't express.
- **No delete endpoints exist in the MVP** (per Phase 2 scope), so cascade-delete behavior on the FKs is intentionally left as the JPA/H2 default (restrict) — there's no code path that would exercise it.

## Seed Data Plan (for NFR3 — demoable on startup)

`data.sql` will seed the three personas from Phase 2 so the app is immediately demoable and screenshot-ready:

| Investor | DOB (age) | Products (name — productType) | Sample withdrawal notices |
|---|---|---|---|
| Thabo Nkosi | 1981-xx-xx (45) | "Unit Trust" — `UNIT_TRUST` (R50,000), "Money Market" — `MONEY_MARKET` (R20,000) | One approved `STANDARD`-category withdrawal |
| Grace van der Merwe | 1958-xx-xx (68) | "Retirement Annuity" — `RETIREMENT_ANNUITY` (R800,000) | One approved `RETIREMENT`-category withdrawal |
| Sipho Dlamini | 1966-xx-xx (60) | "Retirement Annuity" — `RETIREMENT_ANNUITY` (R300,000) | One rejected `RETIREMENT`-category withdrawal (age rule) |

Exact DOB day/month values to be finalized in Phase 7 based on the date the app is actually demoed, so age math stays correct (e.g. Sipho must remain under 65, Grace over 65, regardless of when this is run).

## Deliverables (per Workflow.md Phase 4)
- [x] ERD (this file)
- [x] Database Schema (DDL above)
- [x] Index Strategy

## Exit Criteria
- [x] ERD reviewed and approved.
- [x] Schema includes indexes and constraints.

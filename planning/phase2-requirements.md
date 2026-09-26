# Phase 2: Requirements Gathering — Enviro365 Withdrawal Notice System

Builds on the MVP scope and assumptions locked in [phase1-idea-validation.md](phase1-idea-validation.md).

## Domain Assumptions (needed to make stories concrete; revisit in Phase 4 if wrong)
- One **Investor** has exactly one **Portfolio**.
- A Portfolio holds one or more **Products**, each with a `balance`.
- ~~A **Withdrawal Notice** is submitted against a single Product, has a `type` of either `STANDARD` or `RETIREMENT`, and on approval reduces that product's balance by the withdrawal `amount`.~~ **Superseded (Epic I, added after Epic H):** `type` was a free-choice field on the *withdrawal request* — the client picked `STANDARD` or `RETIREMENT` independently of which product it was withdrawing from, so a withdrawal against a retirement product could be submitted as `STANDARD` and skip the age check entirely. Fixed by giving **Product** a permanent `productType` (a fixed enum, not free text — see Phase 4) whose category (`STANDARD`/`RETIREMENT`) is a property of the product, not something the caller asserts. A Withdrawal Notice still records a `type`, but it's now derived server-side from `product.productType`'s category at submission time, never taken from the request. See phase4's ERD and phase5's revised `WithdrawalRequest`.
- Only `RETIREMENT`-category withdrawals are subject to the age > 65 rule; `STANDARD`-category withdrawals are not. Category is determined by the product's `productType`, not by client input.
- ~~No authentication — the system operates as if a single investor's data is being viewed (investor selected/identified by ID, no login flow).~~ **Superseded (Epic H, added after Epic F):** the user asked for a way to separate accounts rather than an anything-goes investor switcher. Added a **mock login** — email in, matching investor's data out — with no password, token, or session security. This is explicitly identity *resolution* for the demo, not real authentication; the brief never asked for auth, so no security engineering (hashing, sessions, JWTs) was built. See US0 and phase5's `POST /api/auth/login`.

## User Personas

**1. Thabo Nkosi, 45 — Active Investor**
Has two products in his portfolio. Wants to make a standard withdrawal for a portion of his balance. Not eligible for retirement-type withdrawals (under 65) — needs a clear rejection reason if he tries.

**2. Grace van der Merwe, 68 — Retired Investor**
Eligible for retirement withdrawals. Wants to withdraw a large portion of her balance and download a CSV statement of her withdrawal history for her records.

**3. Sipho Dlamini, 60 — Near-Retirement Investor**
Attempts a retirement-type withdrawal before turning 65. Represents the "blocked by business rule" path — exercises the age validation and its error messaging.

## User Stories & Acceptance Criteria

### US0 — Mock Login (added post-Epic F)
*As an investor, I want to log in with my email, so that I only see my own data instead of an open switcher between everyone's accounts.*

- **Given** an email matching a seeded investor, **when** I log in, **then** I land on my own dashboard/history, and my session persists across a page refresh.
- **Given** an email with no matching investor, **when** I log in, **then** I see a clear "No account found for that email" message and stay on the login screen.
- **Given** I am logged in, **when** I click "Log out", **then** my session is cleared and I return to the login screen.

### US1 — View Portfolio
*As an investor, I want to view my portfolio and its products, so that I know my current balances before deciding on a withdrawal.*

- **Given** an investor with a portfolio containing products, **when** the dashboard loads, **then** investor details and all products (name, balance) are displayed.
- **Given** an investor ID that doesn't exist, **when** the portfolio is requested, **then** the API returns 404 with a clear error message and the UI shows a friendly not-found state.

### US2 — Submit a Withdrawal Against a Standard-Category Product
*As an investor, I want to submit a withdrawal request against a product, so that funds are deducted from my balance.*

- **Given** a product with balance R10,000, **when** I request a withdrawal of R5,000, **then** the request succeeds, balance becomes R5,000, and the notice appears in my history.
- **Given** a product with balance R10,000, **when** I request R10,001 (exceeds balance), **then** the API rejects with a clear reason and no balance change occurs.
- **Given** a product with balance R10,000, **when** I request R9,500 (>90% of balance), **then** the API rejects with a clear reason ("exceeds 90% of balance") and no balance change occurs.
- **Given** a product whose `productType` is a standard category (e.g. `UNIT_TRUST`), **when** submitting the withdrawal request, **then** the investor does *not* choose a type — the request only needs `productId` and `amount`.

### US3 — Submit a Withdrawal Against a Retirement-Category Product
*As a retired investor, I want to withdraw from a retirement-category product, so that I can access my funds under retirement rules — without having to declare the type myself.*

**Superseded (Epic I, added after Epic H):** this story previously described the investor picking `RETIREMENT` as a request field. That's gone — the category now comes from the product itself, and the acceptance criteria below reflect that.

- **Given** an investor aged 68 with a product whose `productType` is a retirement category (e.g. `RETIREMENT_ANNUITY`), **when** they submit a withdrawal within balance/90% limits, **then** it succeeds — no `type` field is sent or needed.
- **Given** an investor aged 60 withdrawing from a retirement-category product, **when** they submit the request, **then** the API rejects with reason "Retirement withdrawals require age > 65" regardless of amount, and the UI surfaces that reason — the rule is enforced automatically because of the product's type, not because the investor declared it.

### US4 — View Withdrawal History
*As an investor, I want to see a table of all my past withdrawal notices, so that I can track what's been requested and their outcome.*

- **Given** withdrawals have been submitted (approved and/or rejected), **when** the history table loads, **then** each row shows date, product, type, amount, and status/reason.

### US5 — Export CSV Statement
*As an investor, I want to download a CSV of my withdrawal history with optional filters, so that I have an offline record.*

- **Given** withdrawal history exists, **when** I click "Download CSV" with no filters, **then** a CSV of all withdrawals downloads with correct headers and rows.
- **Given** a date range or type filter is applied, **when** I download, **then** the CSV contains only matching rows.
- **Given** no withdrawals match the filter, **when** I download, **then** a CSV with headers only (no rows) is returned — not an error.

### US6 — Clear Error Feedback
*As an investor, I want clear, specific error messages when a withdrawal is rejected or a request is malformed, so that I understand what to fix.*

- **Given** any business rule violation, **when** the API rejects the request, **then** it returns a structured error (status code + message) that the frontend displays without a raw stack trace or generic failure.
- **Given** malformed input (e.g., negative amount, missing field), **when** submitted, **then** validation errors are returned per-field before any business logic runs.

## Functional Requirements
| ID | Requirement |
|---|---|
| FR1 | `GET` endpoint returns investor details + portfolio products |
| FR2 | `POST` endpoint accepts a withdrawal request, applies all three business rules, calculates new balance, persists the notice |
| FR3 | `GET` endpoint returns withdrawal history for an investor |
| FR4 | `GET` endpoint exports withdrawal history as CSV, supporting filter query params (e.g. date range, type, status) |
| FR5 | Business rules enforced server-side (never trust client validation alone): age > 65 for withdrawals against a retirement-category product (category derived from `product.productType`, never from client input); amount ≤ balance; amount ≤ 90% of balance |
| FR6 | Global exception handling maps validation/business errors to structured JSON error responses with appropriate HTTP status codes |
| FR7 | Frontend dashboard renders portfolio + products from FR1 |
| FR8 | Frontend withdrawal form submits to FR2, performs client-side validation, and displays server-side rejection reasons |
| FR9 | Frontend history table renders data from FR3 |
| FR10 | Frontend CSV download button triggers FR4 and saves the file |

## Non-Functional Requirements
| ID | Requirement |
|---|---|
| NFR1 | REST best practices: resource-based URIs, correct HTTP verbs/status codes (200/201/400/404/422) |
| NFR2 | Layered architecture: Controller → Service (business rules) → Repository (JPA), with a DTO layer separating entities from API contracts |
| NFR3 | H2 database, seeded with sample investor/portfolio/product data so the app is demoable immediately on startup |
| NFR4 | Validation errors (400) and business-rule rejections are distinguishable and both carry human-readable messages |
| NFR5 | Frontend: React (Vite), calling the backend via `fetch`, no hardcoded data once wired up |
| NFR6 | README covers setup steps, API documentation, AI usage disclosure, and screenshots |
| NFR7 | Service-layer business rules are unit-testable in isolation (Mockito-mocked repository, no full Spring context required) |
| NFR8 | Code includes comments explaining non-obvious business-rule decisions, to support the follow-up interview discussion mentioned in the brief |
| NFR9 | Full solution (backend + frontend) delivered within the 7 working-day deadline |

## Deliverables (per Workflow.md Phase 2)
- [x] Requirements Document (this file)
- [x] User Stories
- [x] Acceptance Criteria

## Exit Criteria
- [x] All user personas created.
- [x] User stories written and reviewed.
- [x] Acceptance criteria defined for each story.

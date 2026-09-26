# Phase 3: System Design — Enviro365 Withdrawal Notice System

Builds on [phase1-idea-validation.md](phase1-idea-validation.md) and [phase2-requirements.md](phase2-requirements.md).

## Architecture Overview

A single-instance **monolith**, not microservices — appropriate for the scope (one bounded domain, no independent scaling/deployment needs, 7-day solo build). Two deployables:

- **Backend**: Spring Boot 4.1.1 REST API, layered architecture, embedded H2.
- **Frontend**: React (Vite) SPA, calls the backend exclusively over REST/JSON.

```
┌───────────┐   ┌─────────────┐   ┌──────────┐   ┌──────────────┐   ┌────┐
│ React SPA │──▶│ Controllers │──▶│ Services │──▶│ Repositories │──▶│ H2 │
└───────────┘   └─────────────┘   └──────────┘   └──────────────┘   └────┘
   Browser                   Spring Boot app (assessment)
```

`Controllers → Services → Repositories` all live inside the one Spring Boot app; H2 is the embedded database it manages. No external systems are involved (no payment gateway, email, or third-party APIs) — this is explicitly out of scope per the brief.

## Backend Layering (within the single Spring Boot app)

```
┌───────────────────────┐
│   Controller layer     │   @RestController — HTTP in/out only
└───────────┬────────────┘
            │
            ▼
┌───────────────────────┐
│    Service layer       │   business rules, orchestration, transactions
└───────────┬────────────┘
            │
            ▼
┌───────────────────────┐
│   Repository layer     │   Spring Data JPA interfaces
└───────────┬────────────┘
            │
            ▼
┌───────────────────────┐
│    Entity layer        │   JPA @Entity classes
└───────────────────────┘
```

Cross-cutting concerns (not part of the main call chain, applied at the Controller boundary):

| Concern | Applies to | Purpose |
|---|---|---|
| **DTO layer** | Controller ↔ Service | Request/response shapes — entities never cross the controller boundary |
| **Bean Validation** (`@Valid`) | Controller | Validates request DTOs before the method body runs |
| **GlobalExceptionHandler** (`@ControllerAdvice`) | Controller | Catches exceptions from any layer, returns structured JSON errors |

**Rule**: controllers never touch entities directly, and services never touch `HttpServletRequest`/DTOs' HTTP concerns — keeps each layer independently testable (supports NFR7 from Phase 2).

## Service / Module Boundaries

Three cohesive modules inside the one Spring Boot app (package-level separation, not separate services — no justification here for splitting into microservices):

| Module | Responsibility |
|---|---|
| **Auth module** *(Epic H, added after Epic F)* | Resolves an email to an investor identity (`AuthController`/`AuthService`) — mock login, no password/session |
| **Portfolio module** | Investor, Portfolio, Product entities; read-only retrieval (FR1) |
| **Withdrawal module** | WithdrawalNotice entity; business rule validation (FR5), balance calculation (FR2), history (FR3), CSV export (FR4) |

The Withdrawal module depends on the Portfolio module (needs Product balances and Investor age) — dependency flows one direction only, no circular coupling. The Auth module depends only on `InvestorRepository` and doesn't touch Portfolio/Withdrawal logic at all — it hands the frontend an `investorId` and gets out of the way; every subsequent request still goes through the existing `investorId`-scoped endpoints unchanged.

## Proposed Package Structure (backend)

```
com.enviro.assessment.junior.chadwynprince
├── AssessmentApplication.java
├── config/            # CorsConfig, DataSeeder (H2 seed data on startup)
├── controller/         # InvestorController, WithdrawalController
├── service/            # PortfolioService, WithdrawalService
├── repository/         # InvestorRepository, ProductRepository, WithdrawalNoticeRepository
├── entity/             # Investor, Portfolio, Product, WithdrawalNotice
├── dto/                # request/ and response/ DTOs
├── mapper/             # manual entity <-> DTO mapping (see Phase 1 rationale)
└── exception/          # GlobalExceptionHandler, custom exceptions (e.g. BusinessRuleViolationException)
```

## Proposed Structure (frontend)

React + Vite, plain JavaScript (not TypeScript) — chosen to keep setup/time cost down for a 7-day solo build; component boundaries and PropTypes-level discipline give most of the safety benefit without the tooling overhead.

```
src/
├── api/               # client.js — fetch wrapper, base URL, error normalization
├── components/
│   ├── PortfolioDashboard.jsx
│   ├── WithdrawalForm.jsx
│   ├── WithdrawalHistoryTable.jsx
│   └── CsvDownloadButton.jsx
├── App.jsx
└── main.jsx
```

## External Integrations

None. No payment processors, email/notification services, or third-party APIs are required by the brief. If this were productionized, the natural next integrations would be an identity provider (auth) and a notification service (withdrawal confirmations) — explicitly deferred, not part of this assessment.

## Data Flow Diagrams

### Flow 1 — View Portfolio (US1 / FR1)
```
UI
 └─▶ Controller : GET /api/investors/{id}/portfolio
      └─▶ Service : getPortfolio(id)
           └─▶ Repository : findById(id)
                └─▶ H2 : SELECT investor, portfolio, products
                ◀── H2 : rows
           ◀── Repository : entities
      ◀── Service : PortfolioResponse DTO
 ◀── Controller : 200 OK (JSON)

UI : render dashboard
```

### Flow 2 — Submit Withdrawal (US2/US3 / FR2, FR5, FR6)
> **Superseded by [phase5-api-design.md](phase5-api-design.md):** the `422`/exception-based rejection path below was refined once the DB schema (Phase 4) started persisting every outcome. The final contract returns `201 Created` for both APPROVED and REJECTED — see Phase 5's "Design Reconciliation" section for why.
>
> **Superseded again (Epic I, added after Epic H):** the request body below still shows a client-supplied `type` field. That field is removed — the request is just `{productId, amount}`. The diagram's rule-application step is updated accordingly: the retirement age check now keys off `product.productType`'s category (fetched with the product, not passed in by the caller).
```
UI : client-side validation (amount > 0, required fields)
 └─▶ Controller : POST /api/withdrawals {productId, amount}
      └─▶ Bean Validation : @Valid request DTO

      [IF invalid shape]
      └─▶ GlobalExceptionHandler : MethodArgumentNotValidException
      ◀── GlobalExceptionHandler : 400 {field errors} → UI

      [IF valid shape]
      └─▶ Service : submitWithdrawal(request)
           └─▶ Repository : findProduct(productId), findInvestor(id)
           ◀── Repository : product (incl. productType), investor
      └─▶ Service (self) : apply rules
           • age > 65               (only if product.productType.category = RETIREMENT)
           • amount ≤ balance
           • amount ≤ 90% of balance

           [IF rule violated]
           └─▶ GlobalExceptionHandler : BusinessRuleViolationException(reason)
           ◀── GlobalExceptionHandler : 422 {reason} → UI

           [IF rules pass]
           └─▶ Repository : save(new balance), save(WithdrawalNotice)
                └─▶ H2 : UPDATE product, INSERT withdrawal_notice
      ◀── Service : WithdrawalResponse DTO
 ◀── Controller : 201 Created

UI : show success banner, or rejection reason from response
```

### Flow 3 — Export CSV Statement (US5 / FR4)
```
UI
 └─▶ Controller : GET /api/withdrawals/export?investorId=&from=&to=&type=
      └─▶ Service : exportCsv(filters)
           └─▶ Repository : findByFilters(...)
                └─▶ H2 : SELECT ... WHERE ...
                ◀── H2 : matching rows (possibly empty)
           ◀── Repository : withdrawal notices
      └─▶ Service (self) : build CSV (headers + rows, or headers-only)
      ◀── Service : CSV bytes, Content-Type: text/csv
 ◀── Controller : 200 OK + Content-Disposition: attachment

UI : browser triggers file download
```

## Scaling Requirements

Not a production system — no load, concurrency, or availability targets are specified in the brief, and none are assumed. Explicitly out of scope: horizontal scaling, caching, connection pooling tuning, pagination on large result sets. If this were to go to production, the first steps would be: replace H2 with a real RDBMS (Postgres), add pagination to history/CSV endpoints, and add authentication — all deliberately deferred here to stay inside the MVP.

## Deliverables (per Workflow.md Phase 3)
- [x] Architecture Document (this file)
- [x] Component Diagram (backend layering + module boundaries above)
- [x] Data Flow Diagram (three sequence diagrams above)

## Exit Criteria
- [x] Architecture documented and reviewed.
- [x] All service boundaries and external integrations identified.

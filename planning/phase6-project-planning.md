# Phase 6: Project Planning — Enviro365 Withdrawal Notice System

Breaks the design from Phases 1–5 into a backlog and a day-by-day plan for the 7 working-day deadline (Phase 1 risk #5). Repo layout: a monorepo with `assessment/` (already scaffolded Spring Boot project) for the backend and a new sibling `frontend/` folder for the React app — keeps "backend and frontend" in one GitHub repo as the submission requires, without mixing Maven and Node tooling in the same directory.

## Product Backlog

### Epic A — Backend: Portfolio & Domain Foundation
| ID | Task | Effort |
|---|---|---|
| A1 | Add Spring Web, Spring Data JPA, H2, Validation deps to `pom.xml` | 0.25h |
| A2 | Implement entities (`Investor`, `Portfolio`, `Product`, `WithdrawalNotice`) per Phase 4 schema | 1h |
| A3 | Spring Data JPA repositories | 0.5h |
| A4 | `data.sql` seed data (3 personas from Phase 2/4) | 0.5h |
| A5 | `GET /api/investors/{id}/portfolio` — DTOs, mapper, service, controller | 1.5h |
| A6 | Unit tests: `PortfolioService` (found / not-found, age calculation) | 1h |

### Epic B — Backend: Withdrawal Submission & Business Rules
| ID | Task | Effort |
|---|---|---|
| B1 | `WithdrawalRequest`/`WithdrawalResponse` DTOs + mapper | 0.5h |
| B2 | `WithdrawalService` rule engine (age > 65, ≤ balance, ≤ 90%) | 1.5h |
| B3 | `POST /api/investors/{id}/withdrawals` — Bean Validation + wiring | 1h |
| B4 | Unit tests: all 3 rules incl. boundaries (age=65, amount=balance, amount=90%) | 1.5h |

### Epic C — Backend: History & CSV Export
| ID | Task | Effort |
|---|---|---|
| C1 | `GET /api/investors/{id}/withdrawals` with `type`/`status`/`from`/`to` filters | 1h |
| C2 | CSV export service + `GET .../withdrawals/export` | 1h |
| C3 | Tests: filter combinations, empty-result CSV (headers only, still 200) | 1h |

### Epic D — Backend: Cross-Cutting Concerns
| ID | Task | Effort |
|---|---|---|
| D1 | `GlobalExceptionHandler` (`@ControllerAdvice`) + `ErrorResponse` | 1h |
| D2 | Custom exceptions (`InvestorNotFoundException`, `ProductNotFoundException`) | 0.5h |
| D3 | `springdoc-openapi` wiring → live Swagger UI | 0.5h |

### Epic E — Frontend: Setup & Portfolio Dashboard
| ID | Task | Effort |
|---|---|---|
| E1 | Vite React scaffold in `frontend/` | 0.5h |
| E2 | API client module (`fetch` wrapper, base URL, error normalization) | 0.5h |
| E3 | `PortfolioDashboard` component | 1.5h |

### Epic F — Frontend: Withdrawal Form & History
| ID | Task | Effort |
|---|---|---|
| F1 | `WithdrawalForm` + client-side validation (UI validation, advanced req.) | 2h |
| F2 | `WithdrawalHistoryTable` component | 1h |
| F3 | `CsvDownloadButton` | 0.5h |
| F4 | Success/error feedback wiring (surfaces `rejectionReason`, `ErrorResponse.message`) | 1h |

### Epic G — Documentation & Submission
| ID | Task | Effort |
|---|---|---|
| G1 | README: setup, API docs link, AI usage disclosure | 1h |
| G2 | Screenshots of working app (dashboard, form, rejection, CSV) | 0.5h |
| G3 | Rubric self-review against the brief's checklist before submitting | 0.5h |

### Epic H — Mock Login (added after Epic F, based on user feedback)
Not part of the original Phase 6 backlog — the user asked to separate accounts by a real login screen instead of the open investor-switcher built in Epic E, once F was already complete and verified. See phase2's US0 and phase5's `POST /api/auth/login` addenda.

| ID | Task | Effort |
|---|---|---|
| H1 | Backend: `findByEmailIgnoreCase`, `InvestorNotFoundException(String)` overload, `LoginRequest`/`LoginResponse` DTOs, `AuthService`, `AuthController` | 1h |
| H2 | Backend: `AuthServiceTest` (known email, case-insensitive, unknown email) | 0.5h |
| H3 | Frontend: `login()` in `api/client.js`, `LoginScreen.jsx` (email input + demo-account hint chips) | 1h |
| H4 | Frontend: restructure `App.jsx` around a `session` (login/logout, `localStorage` persistence), remove the old hardcoded investor-switcher | 1h |
| H5 | Styling for the login screen and header/logout affordance | 0.5h |

### Epic I — Standardized Product Types (added after Epic H, based on user feedback)
Not part of the original backlog — the user flagged that `WithdrawalRequest.type` let a client declare `STANDARD`/`RETIREMENT` independently of the product being withdrawn from, bypassing the age rule for a retirement product submitted as `STANDARD`. Fix: `Product` gets a permanent, fixed-enum `productType` (`UNIT_TRUST`, `MONEY_MARKET`, `TAX_FREE_SAVINGS`, `RETIREMENT_ANNUITY`, `PRESERVATION_FUND`); the retirement category is derived from it server-side. See phase2/phase4/phase5's Epic I addenda.

| ID | Task | Effort |
|---|---|---|
| I1 | Backend: `ProductType` enum (+ category mapping), add `productType` column to `Product` entity, migrate `data.sql` seed products | 0.5h |
| I2 | Backend: remove `type` from `WithdrawalRequest`; `WithdrawalService` derives the retirement category from `product.getProductType()` instead of the request | 0.5h |
| I3 | Backend: update/add unit tests — retirement-category product rejects under-65 regardless of any `type` input; standard-category product has no age check | 1h |
| I4 | Backend: `ProductSummary` DTO + mapper gain `productType` | 0.25h |
| I5 | Frontend: `WithdrawalForm` drops the type selector; `PortfolioDashboard`/product list shows `productType` | 0.5h |

**Total estimated effort:** ~21h (Epics A–G) + ~4h (Epic H) + ~2.75h (Epic I) — still comfortably inside the 7 working-day deadline.

## Sprint Plan (7 working days)

| Day | Focus | Epics |
|---|---|---|
| 1 | Backend foundation + portfolio endpoint | A |
| 2 | Withdrawal submission + business rules + tests | B |
| 3 | History, CSV export, exception handling, API docs | C, D |
| 4 | Frontend setup + dashboard | E |
| 5 | Withdrawal form + history table + CSV button + feedback | F |
| 6 | End-to-end integration pass, UI validation polish, backfill test coverage, bug fixing | — |
| 7 | Documentation, screenshots, rubric self-review, submit | G |

## Branching Strategy

Solo build, 7-day deadline — lightweight trunk-based, not full GitFlow:

- **`main`** stays deployable/working at all times.
- One short-lived branch per epic (not per tiny task) — `feat/portfolio-endpoint`, `feat/withdrawal-rules`, `feat/csv-export`, `feat/frontend-dashboard`, `feat/frontend-withdrawal-form`, `docs/readme`.
- Merge to `main` via PR (self-reviewed) even solo — it gives a clean, feature-scoped commit history that's easy to point at in the follow-up interview ("here's the branch where I implemented the 90% rule").
- No release branches, no long-lived `develop` — unnecessary ceremony for a single-developer, single-environment assessment.

## Commit Convention

[Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <summary>`

| Type | Use for |
|---|---|
| `feat` | New endpoint, component, or business rule |
| `fix` | Bug fix |
| `test` | Adding/updating tests |
| `docs` | README, planning docs |
| `refactor` | Non-behavioral code cleanup |
| `chore` | Dependency bumps, build config |

Examples: `feat(withdrawal): enforce 90% balance rule`, `test(withdrawal-service): cover age=65 boundary`, `docs(readme): add AI usage disclosure`.

## GitHub Board

Not created yet — `assessment/` isn't tracked/committed and this repo has no confirmed remote. The table above (Epics A–G, each task a card) maps directly onto a 3-column Kanban (To Do / In Progress / Done) once the repo is pushed to GitHub. Say the word if you'd like this set up as an actual GitHub Project via `gh project create` once there's a remote to attach it to — that's a repo-creation/push action I'll hold off on until you confirm.

## Deliverables (per Workflow.md Phase 6)
- [x] Product Backlog (Epics A–G above)
- [ ] GitHub Board *(deferred — see note above)*
- [x] Sprint Plan
- [x] Branching Strategy

## Exit Criteria
- [x] Backlog is populated and prioritized.
- [x] Branching strategy and commit conventions are documented.

# Phase 1: Idea Validation — Enviro365 Withdrawal Notice System

## Problem Statement
Enviro365 Investments currently processes investor withdrawal notices manually, which is slow and error-prone (no systematic balance/age/percentage checks). The assessment asks us to replace that manual process with a full-stack system so investors can self-serve: view their portfolio, submit a withdrawal request that's validated against business rules in real time, see their withdrawal history, and export statements.

## Target Users
- **Investor** — the only user role implied by the brief. No mention of auth, admin roles, or multi-tenant back-office staff, so this is treated as a single-role system (assumption, flagged below).

## Value Proposition
Automating withdrawal notices removes manual calculation/eligibility errors, gives investors instant feedback (accepted/rejected with a reason) instead of waiting on a back-office review, and produces auditable CSV statements on demand.

## "Competitor" / Reference Analysis
This isn't a market product, so the useful equivalent is: *how would a typical junior-level Spring Boot + SPA CRUD app in this domain be structured?* Standard shape — REST controller → service (business rules) → repository (JPA) → H2, with a thin frontend calling those endpoints. Nothing exotic; the rubric rewards clean layering over cleverness (Code Quality and Spring Boot & Integration are 40% combined).

## Technical Risks
1. **Domain model is underspecified.** "Portfolio", "products", "withdrawal notice" aren't defined with fields/relationships in the brief — reasonable assumptions will be needed in Phase 2/4 (e.g., one investor → one portfolio → many products, each with a balance; a withdrawal notice references a product and deducts from its balance).
2. **No auth is specified.** Assuming no login/security layer is required — scope stays inside "business logic + validation," not identity management. Nothing in the brief asks for it, but worth confirming this doesn't silently cost Integration points.
3. **Spring Boot 4.1.1 / Java 17 pairing.** The existing scaffold pins `spring-boot-starter-parent 4.1.1`. Need to verify at build time that this resolves cleanly against Maven Central and that Java 17 is compatible with Boot 4's baseline — first thing to confirm before adding dependencies.
4. **H2 persistence mode.** In-memory H2 wipes data on restart, which is fine for grading via a fresh run but risky for demoing/screenshotting withdrawal history unless data is seeded via `data.sql` or made file-based. Decide in Phase 4.
5. **7 working-day deadline, solo dev.** Scope creep is the biggest risk to the schedule, not technical difficulty. MVP discipline matters more than breadth.

## MVP Scope (mapped directly to mandatory rubric items)

**Backend (Spring Boot):**
- `GET` investor portfolio (details + products)
- `POST` withdrawal notice (balance calc + the three business rules: age > 65 for retirement, ≤ balance, ≤ 90% of balance)
- `GET` withdrawal history
- `GET` CSV export with filtering

**Frontend:** dashboard, withdrawal form, history table, CSV download button — wired to the above APIs.

**Advanced (need ≥3 of 5 — recommend doing all 5, they're cheap relative to their combined weight):**
- Global exception handling (`@ControllerAdvice`)
- DTO layer (entities never exposed directly over REST)
- Input validation (Jakarta Bean Validation on request DTOs)
- Unit tests (service-layer business rule tests — highest-value tests given the rubric)
- UI validation (client-side checks before hitting the API)

**Out of scope for MVP:** authentication, multi-investor selection/switching UI, pagination, deployment/CI, anything not named in the brief.

## Tech Stack Decision

| Layer | Choice | Justification |
|---|---|---|
| Backend | Spring Boot 4.1.1, Java 17 | Mandated by the brief; already scaffolded with correct package `com.enviro.assessment.junior.chadwynprince`. |
| Persistence | Spring Data JPA + H2 | H2 explicitly required ("Things to Note"). JPA gives clean repository layer with minimal boilerplate. |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) | Satisfies "Input validation" advanced requirement with standard, well-understood annotations. |
| API layer | Spring Web (`spring-boot-starter-web`) | Needed for REST controllers; not yet in the scaffolded `pom.xml`. |
| DTO mapping | Manual mapper methods (no MapStruct/ModelMapper) | Small number of DTOs; manual mapping is easier to explain in a follow-up interview than a codegen library, and keeps dependencies minimal. |
| Testing | JUnit 5 + Mockito (already pulled in via `spring-boot-starter-test`) | No extra dependency needed; standard for service-layer rule tests. |
| Frontend | React (Vite) | Chosen over plain HTML/JS and Angular: strongest industry-skill signal for a junior hire while staying lighter than Angular's boilerplate for a solo 7-day build. |

## Deliverables (per Workflow.md Phase 1)
- [x] Problem Statement
- [x] Value Proposition
- [x] MVP Feature List
- [x] Risk Assessment
- [x] Tech Stack Decision

## Exit Criteria
- [x] Problem and value proposition are clearly documented.
- [x] MVP scope is agreed and bounded.
- [x] Tech stack is decided and justified.

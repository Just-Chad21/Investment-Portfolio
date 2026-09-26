# Phase 5: API Design — Enviro365 Withdrawal Notice System

Builds on [phase3-system-design.md](phase3-system-design.md) (flows) and [phase4-database-design.md](phase4-database-design.md) (schema).

## Design Reconciliation: how a rejected withdrawal is reported

Phase 3's sequence diagram modeled a business-rule failure (age/balance/90%) as a thrown exception mapped to `422`. Phase 4's schema instead persists **every** withdrawal attempt with a `status` (`APPROVED`/`REJECTED`) and `rejectionReason` — i.e. a rejection is a valid, recorded domain outcome, not a processing failure.

API Design formalizes the schema's view as the actual contract: **`POST /withdrawals` returns `201 Created` whether the outcome is APPROVED or REJECTED** — a WithdrawalNotice resource was successfully created either way, and the frontend reads `status`/`rejectionReason` in the body to decide what to show. Exceptions (`400`/`404`) are reserved for requests that never became a valid domain outcome at all (malformed input, unknown investor/product). This keeps business-rule flow control out of the exception hierarchy — exceptions are for actual errors, not expected branches.

## Design Reconciliation: where the retirement category comes from (Epic I, added after Epic H)

Originally `WithdrawalRequest` carried its own `type` field (`STANDARD`/`RETIREMENT`), chosen by the client independently of the product being withdrawn from — so a request against a retirement product could self-declare `STANDARD` and skip the age > 65 rule. Phase 4 closed that gap by giving `Product` a permanent `productType` (fixed enum: `UNIT_TRUST`, `MONEY_MARKET`, `TAX_FREE_SAVINGS`, `RETIREMENT_ANNUITY`, `PRESERVATION_FUND`), each mapped to a `STANDARD` or `RETIREMENT` category.

API Design formalizes the consequence: **`WithdrawalRequest` no longer has a `type` field** — it's just `{productId, amount}`. The service looks up the product, reads its `productType`'s category, and applies the age rule only when that category is `RETIREMENT`. `WithdrawalResponse` still returns a `type` (`STANDARD`/`RETIREMENT`) per notice, and history/CSV export/filtering by `type` are unchanged from the caller's point of view — only the *request* shape changed, because the value is now derived, not asserted. `PortfolioResponse`'s `ProductSummary` gains a `productType` field so the frontend can label products and (optionally) hint in the UI that a retirement product is age-gated, without the investor ever having to pick a type.

## Endpoint Summary

| Method | Path | Purpose | Maps to |
|---|---|---|---|
| `POST` | `/api/auth/login` | Resolve an email to an investor identity (mock login, Epic H) | US0 |
| `GET` | `/api/investors/{investorId}/portfolio` | Investor details + products | FR1 |
| `GET` | `/api/investors/{investorId}/withdrawals` | Withdrawal history (filterable) | FR3 |
| `POST` | `/api/investors/{investorId}/withdrawals` | Submit a withdrawal notice | FR2, FR5 |
| `GET` | `/api/investors/{investorId}/withdrawals/export` | CSV export (filterable) | FR4 |

The four investor-scoped endpoints are nested under the investor resource — consistent hierarchy, no anonymous top-level `/api/withdrawals`, and it removes any ambiguity about which investor a withdrawal belongs to (NFR1). `/api/auth/login` is deliberately not nested under `/api/investors/{id}` — there's no `investorId` yet at login time; it's the one endpoint whose job is to produce one.

## Endpoint Detail

### 0. `POST /api/auth/login` *(Epic H, added after Epic F)*
**Not real authentication** — no password, token, or session. Resolves an email to the matching investor's identity so the frontend can drive the existing `investorId`-scoped endpoints. Email match is case-insensitive.

**Request body**
```json
{ "email": "thabo.nkosi@example.com" }
```
**Response `200 OK`**
```json
{ "investorId": 1, "firstName": "Thabo", "lastName": "Nkosi" }
```
**`400 Bad Request`** — blank or malformed email (Bean Validation `@NotBlank @Email`).
**`404 Not Found`** — no investor with that email.

---

### 1. `GET /api/investors/{investorId}/portfolio`
**Response `200 OK`**
```json
{
  "investorId": 1,
  "firstName": "Thabo",
  "lastName": "Nkosi",
  "age": 45,
  "portfolioNumber": "PF-0001",
  "products": [
    { "productId": 10, "name": "Unit Trust", "productType": "UNIT_TRUST", "balance": 50000.00 },
    { "productId": 11, "name": "Money Market", "productType": "MONEY_MARKET", "balance": 20000.00 }
  ]
}
```
**`404 Not Found`** — no investor with that ID.

`age` is computed server-side from `dateOfBirth` at request time (never stored), so it's always correct regardless of when the app is demoed.

---

### 2. `GET /api/investors/{investorId}/withdrawals`
**Query params (all optional):** `type` (`STANDARD`\|`RETIREMENT`), `status` (`APPROVED`\|`REJECTED`), `from` (`yyyy-MM-dd`), `to` (`yyyy-MM-dd`)

**Response `200 OK`** — `WithdrawalResponse[]`, newest first:
```json
[
  {
    "id": 102,
    "productId": 12,
    "productName": "Retirement Annuity",
    "type": "RETIREMENT",
    "amount": 100000.00,
    "status": "REJECTED",
    "rejectionReason": "Retirement withdrawals require age > 65 (investor is 60)",
    "balanceAfter": null,
    "requestedAt": "2026-09-22T10:20:00"
  }
]
```
**`400 Bad Request`** — malformed `from`/`to`, or `from` after `to`.
**`404 Not Found`** — no investor with that ID.

---

### 3. `POST /api/investors/{investorId}/withdrawals`
**Request body** — no `type` field: the category is derived server-side from `product.productType`, never supplied by the client (see Design Reconciliation above).
```json
{
  "productId": 10,
  "amount": 5000.00
}
```
**Response `201 Created`** (both outcomes use the same shape — see reconciliation note above)
```json
{
  "id": 101,
  "productId": 10,
  "productName": "Unit Trust",
  "type": "STANDARD",
  "amount": 5000.00,
  "status": "APPROVED",
  "rejectionReason": null,
  "balanceAfter": 45000.00,
  "requestedAt": "2026-09-22T10:15:30"
}
```
**`400 Bad Request`** — request fails Bean Validation (see rules below) *before* any business rule is evaluated.
**`404 Not Found`** — investor doesn't exist, `productId` doesn't exist, or `productId` doesn't belong to this investor's portfolio (returning 404 rather than 403 avoids confirming another investor's product ID exists).

---

### 4. `GET /api/investors/{investorId}/withdrawals/export`
Same query params as endpoint 2.

**Response `200 OK`**, `Content-Type: text/csv`, `Content-Disposition: attachment; filename="withdrawals.csv"`
```csv
id,productName,type,amount,status,rejectionReason,balanceAfter,requestedAt
102,Retirement Annuity,RETIREMENT,100000.00,REJECTED,"Retirement withdrawals require age > 65 (investor is 60)",,2026-09-22T10:20:00
101,Unit Trust,STANDARD,5000.00,APPROVED,,45000.00,2026-09-22T10:15:30
```
No matching rows → headers only, still `200 OK` (per US5 acceptance criteria, Phase 2 — an empty result is not an error).

**`400`/`404`** — same as endpoint 2.

## Request/Response DTOs

| DTO | Fields |
|---|---|
| `LoginRequest` | `email (String, required, must be a valid email)` |
| `LoginResponse` | `investorId, firstName, lastName` |
| `PortfolioResponse` | `investorId, firstName, lastName, age, portfolioNumber, products: ProductSummary[]` |
| `ProductSummary` | `productId, name, productType, balance` |
| `WithdrawalRequest` | `productId (Long, required), amount (BigDecimal, required)` — no `type`; derived server-side from the product's `productType` |
| `WithdrawalResponse` | `id, productId, productName, type, amount, status, rejectionReason, balanceAfter, requestedAt` |
| `ErrorResponse` | `timestamp, status, error, message, path, fieldErrors?: FieldError[]` |
| `FieldError` | `field, message` |

## Validation Rules

| Field | Rule | Failure |
|---|---|---|
| `email` (login body) | required; must be a well-formed email; matched case-insensitively | `400` (malformed) / `404` (no match) |
| `investorId` (path) | must reference an existing investor | `404` |
| `productId` (body) | required; must exist; must belong to `investorId`'s portfolio | `400` (missing) / `404` (not found or not owned) |
| `amount` (body) | required; `> 0`; at most 2 decimal places | `400` |
| `from`, `to` (query) | optional; ISO `yyyy-MM-dd`; if both present, `from ≤ to` | `400` |

Everything above is **shape/existence** validation, enforced by Bean Validation (`@Valid`, `@NotNull`, `@Positive`, custom `@ExistsAsProduct` or a service-layer pre-check) before the service method runs. The three **business rules** (age > 65 for RETIREMENT, amount ≤ balance, amount ≤ 90% of balance) are evaluated *inside* the service after validation passes, and — per the reconciliation above — produce a `REJECTED` result, not a `400`.

## Error Handling (`GlobalExceptionHandler`)

| Exception | Status | Body |
|---|---|---|
| `MethodArgumentNotValidException` | `400` | `ErrorResponse` with `fieldErrors` populated |
| `InvestorNotFoundException` | `404` | `ErrorResponse`, message names the missing investor (by id or, for login, by email) |
| `ProductNotFoundException` (incl. wrong-owner case) | `404` | `ErrorResponse` |
| Any other unhandled exception | `500` | Generic `ErrorResponse` — no stack trace, no internal details leaked |

All four still share the same `ErrorResponse` shape, so the frontend has exactly one error-handling code path (US6, FR6).

## OpenAPI Specification

```yaml
openapi: 3.0.3
info:
  title: Enviro365 Withdrawal Notice API
  version: "1.0"
paths:
  /api/auth/login:
    post:
      summary: Resolve an email to an investor identity (mock login, not real auth)
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: "#/components/schemas/LoginRequest" }
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema: { $ref: "#/components/schemas/LoginResponse" }
        "400": { $ref: "#/components/responses/BadRequest" }
        "404": { $ref: "#/components/responses/NotFound" }

  /api/investors/{investorId}/portfolio:
    get:
      summary: Get investor portfolio
      parameters:
        - name: investorId
          in: path
          required: true
          schema: { type: integer, format: int64 }
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema: { $ref: "#/components/schemas/PortfolioResponse" }
        "404":
          description: Investor not found
          content:
            application/json:
              schema: { $ref: "#/components/schemas/ErrorResponse" }

  /api/investors/{investorId}/withdrawals:
    get:
      summary: List withdrawal history
      parameters:
        - name: investorId
          in: path
          required: true
          schema: { type: integer, format: int64 }
        - name: type
          in: query
          schema: { type: string, enum: [STANDARD, RETIREMENT] }
        - name: status
          in: query
          schema: { type: string, enum: [APPROVED, REJECTED] }
        - name: from
          in: query
          schema: { type: string, format: date }
        - name: to
          in: query
          schema: { type: string, format: date }
      responses:
        "200":
          description: OK
          content:
            application/json:
              schema:
                type: array
                items: { $ref: "#/components/schemas/WithdrawalResponse" }
        "400": { $ref: "#/components/responses/BadRequest" }
        "404": { $ref: "#/components/responses/NotFound" }
    post:
      summary: Submit a withdrawal notice
      parameters:
        - name: investorId
          in: path
          required: true
          schema: { type: integer, format: int64 }
      requestBody:
        required: true
        content:
          application/json:
            schema: { $ref: "#/components/schemas/WithdrawalRequest" }
      responses:
        "201":
          description: Created (APPROVED or REJECTED outcome)
          content:
            application/json:
              schema: { $ref: "#/components/schemas/WithdrawalResponse" }
        "400": { $ref: "#/components/responses/BadRequest" }
        "404": { $ref: "#/components/responses/NotFound" }

  /api/investors/{investorId}/withdrawals/export:
    get:
      summary: Export withdrawal history as CSV
      parameters:
        - name: investorId
          in: path
          required: true
          schema: { type: integer, format: int64 }
        - name: type
          in: query
          schema: { type: string, enum: [STANDARD, RETIREMENT] }
        - name: status
          in: query
          schema: { type: string, enum: [APPROVED, REJECTED] }
        - name: from
          in: query
          schema: { type: string, format: date }
        - name: to
          in: query
          schema: { type: string, format: date }
      responses:
        "200":
          description: CSV file
          content:
            text/csv:
              schema: { type: string }
        "400": { $ref: "#/components/responses/BadRequest" }
        "404": { $ref: "#/components/responses/NotFound" }

components:
  responses:
    BadRequest:
      description: Validation failure
      content:
        application/json:
          schema: { $ref: "#/components/schemas/ErrorResponse" }
    NotFound:
      description: Resource not found
      content:
        application/json:
          schema: { $ref: "#/components/schemas/ErrorResponse" }

  schemas:
    LoginRequest:
      type: object
      required: [email]
      properties:
        email: { type: string, format: email }

    LoginResponse:
      type: object
      properties:
        investorId: { type: integer, format: int64 }
        firstName: { type: string }
        lastName: { type: string }

    ProductSummary:
      type: object
      properties:
        productId: { type: integer, format: int64 }
        name: { type: string }
        productType: { type: string, enum: [UNIT_TRUST, MONEY_MARKET, TAX_FREE_SAVINGS, RETIREMENT_ANNUITY, PRESERVATION_FUND] }
        balance: { type: number, format: double }

    PortfolioResponse:
      type: object
      properties:
        investorId: { type: integer, format: int64 }
        firstName: { type: string }
        lastName: { type: string }
        age: { type: integer }
        portfolioNumber: { type: string }
        products:
          type: array
          items: { $ref: "#/components/schemas/ProductSummary" }

    WithdrawalRequest:
      type: object
      required: [productId, amount]
      properties:
        productId: { type: integer, format: int64 }
        amount: { type: number, format: double, minimum: 0, exclusiveMinimum: true }

    WithdrawalResponse:
      type: object
      properties:
        id: { type: integer, format: int64 }
        productId: { type: integer, format: int64 }
        productName: { type: string }
        type: { type: string, enum: [STANDARD, RETIREMENT] }
        amount: { type: number, format: double }
        status: { type: string, enum: [APPROVED, REJECTED] }
        rejectionReason: { type: string, nullable: true }
        balanceAfter: { type: number, format: double, nullable: true }
        requestedAt: { type: string, format: date-time }

    FieldError:
      type: object
      properties:
        field: { type: string }
        message: { type: string }

    ErrorResponse:
      type: object
      properties:
        timestamp: { type: string, format: date-time }
        status: { type: integer }
        error: { type: string }
        message: { type: string }
        path: { type: string }
        fieldErrors:
          type: array
          items: { $ref: "#/components/schemas/FieldError" }
```

**Implementation note:** rather than hand-maintaining this YAML alongside the code, Phase 7 will add `springdoc-openapi-starter-webmvc-ui` so the live spec (`/v3/api-docs`) and Swagger UI (`/swagger-ui.html`) are generated from the actual controllers/DTOs — this file is the design-time contract those annotations will implement, and README documentation will link to the live UI rather than duplicate it.

## Deliverables (per Workflow.md Phase 5)
- [x] API Specification (OpenAPI YAML above)
- [x] Endpoint Documentation (per-endpoint detail above)

## Exit Criteria
- [x] All endpoints documented with request/response structures.
- [x] Validation rules and error handling defined.
- [x] OpenAPI spec generated.

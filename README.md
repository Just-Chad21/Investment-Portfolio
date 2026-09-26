# Investment-Portfolio

Enviro365's Withdrawal Notice Portal — investors log in, see their portfolio (a mix of
standard and retirement products), and submit withdrawal requests against it. Every
request is evaluated and persisted as either **approved** or **rejected**, with a reason,
so the withdrawal history is a full audit trail, not just a record of successes.

Two rules govern every withdrawal, enforced server-side in
[`WithdrawalService`](assessment/src/main/java/com/enviro/assessment/junior/chadwynprince/service/WithdrawalService.java)
(the source of truth — this is just an orientation, not a restatement):

- **Retirement products** (Retirement Annuity, Preservation Fund) can only be withdrawn
  from once the investor is over 65.
- **No withdrawal can exceed 90% of a product's balance**, regardless of type.

## Architecture

Two independently runnable services, wired together by Docker Compose:

- **`assessment/`** — Spring Boot + an in-memory H2 database. Owns the data model, the
  two rules above, and the REST API. Nothing to provision; H2 is seeded on startup from
  [`data.sql`](assessment/src/main/resources/data.sql).
- **`frontend/`** — React + Vite single-page app. Talks to the backend over `fetch`; no
  server-side rendering, no separate backend-for-frontend.

There's no real authentication — login just resolves an email to an investor record (see
the comment on `AuthService` for why that's deliberate). There's no self-registration
either, which is why the login screen offers three seeded demo accounts instead.

## Demo accounts

Three investors are seeded on backend startup, each set up to exercise a different rule
path rather than being interchangeable test users:

| Investor | Age | Holds | Demonstrates |
|---|---|---|---|
| Thabo Nkosi | 45 | Standard products only | The 90% cap, on its own |
| Grace van der Merwe | 68 | Standard + retirement | Retirement withdrawals succeeding (eligible) |
| Sipho Dlamini | 60 | Standard + retirement | Retirement withdrawals rejected (under 65) |

Full detail, including their existing withdrawal history, is in `data.sql`.

## Running it

### Docker Compose (both services)

```
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080

The frontend is published on 5173 (not nginx's default 80) so its origin matches the
backend's CORS config (`http://localhost:5173`) with no extra configuration.

The frontend's API base URL is baked in at build time. If the backend needs to run
somewhere other than `http://localhost:8080`, rebuild with:

```
docker compose build --build-arg VITE_API_BASE_URL=http://your-backend-host:8080 frontend
```

### Running a service standalone (local dev)

```
# backend — http://localhost:8080
cd assessment && mvn spring-boot:run

# frontend — http://localhost:5173, with hot reload
cd frontend && npm install && npm run dev
```

## API documentation

With the backend running (either way above), interactive API docs are generated from the code:

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI spec (JSON): http://localhost:8080/v3/api-docs

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/auth/login` | Resolve an email to an investor (no password) |
| `GET` | `/api/investors/{id}/portfolio` | Investor details, products and balances, withdrawal rule limits |
| `POST` | `/api/investors/{id}/withdrawals` | Submit a withdrawal; returns `201` with status `APPROVED` or `REJECTED` |
| `GET` | `/api/investors/{id}/withdrawals` | History, newest first; optional `type`, `status`, `from`, `to` filters |
| `GET` | `/api/investors/{id}/withdrawals/export` | Same history as a CSV download, same filters |

A rule violation is not an HTTP error: it returns `201` with `status: REJECTED` and a
`rejectionReason`, because rejected attempts are recorded too. Error statuses are reserved for
invalid input (`400`, with per-field `fieldErrors`) and unknown investors or products (`404`).

## Running the tests

```
cd assessment && mvn test
```

- **Unit tests** (`service/`) cover the business rules in isolation, including the
  boundaries: age exactly 65 is rejected, exactly 90% of the balance is approved.
- **Integration tests** (`ApiIntegrationTest`) boot the full application and exercise every
  endpoint over HTTP against the seeded database: real queries and filters, validation and
  error responses, and the CSV download. Each test rolls back, so they don't affect each other.

## AI usage

This project was built with the help of Claude Code (Anthropic's AI coding assistant), as the
`Co-Authored-By` trailers on the commits show. I set the requirements and direction, reviewed
the output, and asked for changes where the result wasn't right. For example, I asked for
the mock login, and for the retirement rule to come from the product type instead of the request.

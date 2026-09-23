# Investment-Portfolio

## Run with Docker Compose

```
docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080

The frontend is published on 5173 (not nginx's default 80) so its origin matches the
backend's CORS config (`http://localhost:5173`) with no extra configuration. The backend
uses an in-memory H2 database seeded on startup, so there's no database to provision.

The frontend's API base URL is baked in at build time. If the backend needs to run
somewhere other than `http://localhost:8080`, rebuild with:

```
docker compose build --build-arg VITE_API_BASE_URL=http://your-backend-host:8080 frontend
```

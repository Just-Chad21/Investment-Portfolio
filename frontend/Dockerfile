# syntax=docker/dockerfile:1

# ---- Build ----
FROM node:22-alpine AS build
WORKDIR /build
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
# Baked in at build time (Vite inlines import.meta.env.* into the bundle) — the browser,
# not the frontend container, is what calls this URL, so it must be reachable from the
# host, not just from inside the compose network.
ARG VITE_API_BASE_URL=http://localhost:8080
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
RUN npm run build

# ---- Run ----
FROM nginx:1.27-alpine AS run
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80

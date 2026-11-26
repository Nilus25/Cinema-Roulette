# IMDBSelector

A personal movie and TV show watchlist manager and random-pick web application built around IMDb CSV exports.

## Overview

IMDBSelector solves decision fatigue when your watchlist grows too large:
- **User Profiles**: Create, load, and delete viewer profiles, each tracking seen titles and the last randomly picked title.
- **IMDb CSV Watchlists**: Import and update watchlists from standard IMDb CSV exports (header-mapped and resilient to missing ratings/years).
- **Filtering**: Filter by rating range (`0.0`–`10.0`), release year range, genres, media types, title/director keywords, and **Not-Seen-Only** mode.
- **Random Pick**: Select a random matching title, view its details, open its IMDb page, and mark it as seen.

---

## Tech Stack & Architecture

- **Backend (`backend/`)**: **Java 25**, **Javalin**, **jOOQ** (compile-time SQL codegen from Flyway migrations), **Flyway**, **PostgreSQL**, **OpenCSV**, **Gradle (Kotlin DSL)**.
  - Structured with **Hexagonal Architecture (Ports & Adapters)** and **DDD** (pure `domain` enforced by **ArchUnit**, `application` use cases & ports, `infrastructure` adapters wired manually in `AppAssembler`).
  - **RFC 9457 Problem Details** error responses and `X-Trace-Id` request correlation/timing logs.
- **Frontend (`frontend/`)**: **React**, **TypeScript**, **Vite**, **Tailwind CSS** (mobile-first responsive SPA).
- **Infrastructure**: **Docker Compose** (PostgreSQL 17) and multi-stage **Dockerfile** for the backend.

See [documentation/target-architecture.md](documentation/target-architecture.md) and [documentation/roadmap.md](documentation/roadmap.md) for architecture diagrams and upcoming features.

---

## Running Locally

### Prerequisites
- **Docker** (for PostgreSQL)
- **Java 25** (or Java 11+ with Gradle toolchain auto-provisioning)
- **Node.js 20+**

### 1. Start PostgreSQL
```sh
docker compose up -d
```

### 2. Start the Backend (port `8080`)
```sh
./backend/gradlew -p backend run
```

### 3. Start the Frontend (port `5173`)
```sh
cd frontend
npm install
npm run dev
```

### Optional: Migrate Legacy JSON Data
If you have legacy `*_user.json` and `*_watchlist.json` files under `data/`:
```sh
./backend/gradlew -p backend run -PmainClass=com.imdbselector.infrastructure.adapter.in.cli.LegacyJsonMigrator
```

---

## Running Tests
```sh
./backend/gradlew -p backend test
cd frontend && npm run build
```

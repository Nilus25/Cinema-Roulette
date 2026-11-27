# Cinema Roulette

*Formerly IMDBSelector* — a personal movie and TV show watchlist manager and random-pick web application built around IMDb CSV exports.

## Why I Built This

My IMDb watchlist grew so large that I was spending more time deciding what to watch than actually watching anything.

Cinema Roulette started as a quick Java 11 Swing desktop app (`IMDBSelector`) with local JSON files. I modernized it into a decoupled web application (`v2.0`) to make it accessible from anywhere (including mobile) and to apply clean architecture, DDD, OWASP security practices, and modern Java/React tooling. *(The original Swing version is preserved under the [`legacy-v1`](../../tree/legacy-v1) tag.)*

## Features

- **Authenticated User Profiles**: Password-protected accounts (BCrypt + `HttpOnly` JWT cookies), each tracking seen titles (`Recently Screened`) and the last randomly picked title.
- **IMDb CSV Import & Update Diff Preview**: Import watchlists from standard IMDb CSV exports and preview added/removed titles before applying an update.
- **Filtering & 35mm Roulette**: Filter by rating range (`0.0`–`10.0`), release year, genres, media types, title/director keywords, and **Include Seen** toggle, then spin the filmstrip reel to pick a title and open its IMDb page.

## Project Structure

```text
├── backend/                 # Java 25, Javalin, jOOQ, Flyway, PostgreSQL 17 (Hexagonal + DDD)
├── frontend/                # React, TypeScript, Vite, Tailwind CSS
├── scripts/                 # PostgreSQL backup & dev helper scripts
├── Caddyfile                # Production reverse proxy with automatic HTTPS
├── docker-compose.yml       # Local PostgreSQL, backend, and frontend stack
└── docker-compose.prod.yml  # Production overrides with Caddy
```

## Getting Started

### Prerequisites

- **Docker** (for PostgreSQL or the full stack)
- **Java 25** (or Java 11+ via Gradle toolchain, when running the backend outside Docker)
- **Node.js 22+** (when running the frontend outside Docker)

### Run with Docker Compose

```sh
docker compose up -d --build
```

Then open `http://localhost:5173`.

### Run Locally for Development

1. Start PostgreSQL:
   ```sh
   docker compose up -d postgres
   ```

2. Start the backend (`http://localhost:8080`):
   ```sh
   ./backend/gradlew -p backend run
   ```

3. Start the frontend (`http://localhost:5173`):
   ```sh
   cd frontend
   npm install
   npm run dev
   ```

<details>
<summary>Optional: Migrate legacy JSON files</summary>

If you have legacy `*_user.json` and `*_watchlist.json` files under `data/`:
```sh
./backend/gradlew -p backend run -PmainClass=com.imdbselector.infrastructure.adapter.in.cli.LegacyJsonMigrator
```

</details>

## Testing

```sh
# Backend unit, HTTP, CSV, and ArchUnit architecture tests
./backend/gradlew -p backend test

# Frontend typecheck and production build
cd frontend && npm run build
```

## License

See [LICENCE](LICENCE).

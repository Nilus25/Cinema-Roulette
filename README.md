# IMDBSelector

A personal movie and TV show watchlist manager and random-pick web application built around IMDb CSV exports.

## Why I Built This

My IMDb watchlist grew so large that I was spending more time deciding what to watch than actually watching anything.

IMDBSelector started as a quick Java 11 Swing desktop app with local JSON files. I later modernized it into a decoupled web application to make it accessible from anywhere (including mobile) and to apply clean architecture, DDD, and modern Java/React practices. *(The original Swing version is preserved under the [`legacy-v1`](../../tree/legacy-v1) tag.)*

## Features

- **User Profiles**: Create, load, and delete viewer profiles, each tracking seen titles and the last randomly picked title.
- **IMDb CSV Import & Update**: Import watchlists from standard IMDb CSV exports and replace/update them cleanly.
- **Filtering**: Filter by rating range (`0.0`–`10.0`), release year, genres, media types, title/director keywords, and **Not-Seen-Only** mode.
- **Random Pick**: Pick a random matching title, open its IMDb page, and mark it as seen (or get prompted on next load if you watched your last pick).

## Project Structure

```text
├── backend/            # Java 25, Javalin, jOOQ, Flyway, PostgreSQL (Hexagonal + DDD)
├── frontend/           # React, TypeScript, Vite, Tailwind CSS
└── docker-compose.yml  # Local PostgreSQL 17 setup
```

## Getting Started

### Prerequisites

- **Docker** (for PostgreSQL)
- **Java 25** (or Java 11+ via Gradle toolchain)
- **Node.js 20+**

### Run Locally

1. Start PostgreSQL:
   ```sh
   docker compose up -d
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

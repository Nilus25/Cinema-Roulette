# IMDBSelector

## Description
IMDBSelector is a personal app for managing IMDb watchlists and picking something to watch without spending half an hour scrolling.

You can create profiles to keep track of what you've already seen, import watchlists from IMDb CSV exports, filter by genre, rating, year, media type, or keywords, and pick a random title (either from the whole filtered list or only from titles you haven't seen yet). It also remembers the last title it picked so you can mark it as seen next time you open it.

## Motivation
I originally built this to solve my own boredom problem: my IMDb watchlist got so huge that I was spending more time deciding what to watch than actually watching anything.

It started as a quick Java 11 Swing desktop app with local JSON files, and I've since modernized it into a web app with a Java 25 backend, PostgreSQL, and a React frontend—both to use it from anywhere (including my phone) and to practice cleaner architecture and modern tooling.

## Requirements
- **Docker** (for PostgreSQL)
- **Java 25** (or Java 11+ with Gradle toolchain)
- **Node.js 20+**

## Usage

1. Start PostgreSQL:
```sh
docker compose up -d
```

2. Start the backend:
```sh
./backend/gradlew -p backend run
```

3. Start the frontend:
```sh
cd frontend
npm install
npm run dev
```

*(If you have legacy `*_user.json` and `*_watchlist.json` files under `data/`, you can import them with `./backend/gradlew -p backend run -PmainClass=com.imdbselector.infrastructure.adapter.in.cli.LegacyJsonMigrator`.)*

# GEMINI.md — IMDBSelector

## 1. What IMDBSelector Is & Does

**IMDBSelector** is a personal movie and TV show watchlist management and random-pick application built around IMDB CSV exports.

### Core Capabilities
- **User Profiles**: Create, load, and delete local user profiles. Each profile tracks the user's `seenMovies` list and `lastMovieGenerated`.
- **Watchlist Management**: Import watchlists from IMDB-exported CSV files, update/replace an existing watchlist's movies from a new CSV, view all movies in a watchlist (with seen/unseen status), and delete watchlists.
- **Filtering**: Filter the active watchlist by:
  - **Rating range** (`0.0` – `10.0`)
  - **Release year range** (`1890` – current year)
  - **Genres** (inclusive across selected genres, e.g., Action, Drama, Sci-Fi)
  - **Media types** (inclusive across selected types, e.g., Movie, TV Series, Short)
  - **Keywords** (case-insensitive substring match on title or director)
- **Random Movie Selection**:
  - **Random mode**: Picks a random title from the filtered watchlist.
  - **Not-seen mode**: Excludes movies already in the active user's `seenMovies` before picking.
  - Stores the picked title as `lastMovieGenerated` so the user can be prompted to mark it as seen on next load, and opens/links its IMDB URL.

---

## 2. Current State vs. Modernization Target

- **Current State (Legacy)**: Single-process Java 11 Swing desktop app (`Presentation`, `Domain`, `DataLayer`) with IntelliJ `.form` files, vendored JARs in `lib/`, local JSON persistence via Gson (`data/user`, `data/watchlist`), and inverted dependencies (`Domain` directly instantiates `MainView` and `UserGateway`/`WatchListGateway`). See [documentation/current-architecture.md](documentation/current-architecture.md) for full UML diagrams.
- **Target Architecture**:
  - **Separated Backend & Web Frontend**: Headless REST API backend + standalone web application frontend.
  - **Hexagonal Architecture (Ports & Adapters) & DDD**: Pure domain core with explicit inbound/outbound ports, rich value objects/entities, and swappable adapters.
  - **Structured Persistence**: Real relational/structured database instead of raw JSON files.
  - **Pragmatic Scope**: Standard synchronous REST application. No event-driven architecture, message brokers, or multi-user auth complexity unless explicitly requested — while still keeping clean boundaries so the system is easy to extend.

---

## 3. Mandatory Collaboration & Decision Rules

When working on this repository, **you MUST**:

1. **Always ask for requirements**: Never invent or assume product requirements, edge-case behaviors, or scope expansions. Clarify with the user first.
2. **Always ask for technology decisions**: Before introducing or committing to a language, framework, frontend stack, database, build tool, or library, present the trade-offs and let the user decide.
3. **Always ask for design decisions**: Before implementing architectural structures (aggregate boundaries, API endpoints/contracts, database schema, module layout, or domain modeling choices), propose options and ask the user to confirm.
4. **Respect Hexagonal & DDD boundaries** (once migration begins):
   - The **Domain** layer must have zero imports from web frameworks, UI, persistence, or `java.awt`.
   - Side effects (persistence, CSV parsing, external lookups) live behind **Outbound Ports** and **Adapters**.
   - **Application Use Cases** orchestrate domain logic and ports without leaking transport details (HTTP/JSON) into the domain.

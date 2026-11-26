# Product Roadmap & Feature Priorities

## v1 — Core Modernization (Current Milestone)
- **Hexagonal + DDD Backend** (Java 25, Javalin, jOOQ, Flyway, PostgreSQL)
- **Decoupled Web Frontend** (React + TypeScript + Vite)
- **Feature Parity**: User profiles, header-resilient IMDB CSV import/update/delete, multi-criteria filtering, Random & Not-Seen selection, seen-list management, and `lastGenerated` tracking.

---

## Post-v1 Prioritized Backlog

| Priority | Feature / Upgrade | Notes |
| :---: | :--- | :--- |
| **0 (Top)** | **Web Deployment & Mobile-Friendly Access** | Host backend + frontend on the web with responsive mobile UI so it is accessible from a phone anywhere. (Requires HTTPS, CORS/reverse proxy, and pushes Multi-user Auth/Ownership up in importance when exposed publicly.) |
| **1** | **TMDB / OMDb Enrichment** | Behind an outbound `MediaEnrichmentPort` (e.g. `TmdbMetadataAdapter` / `OmdbMetadataAdapter`): fetch posters, plot synopses, runtime, Rotten Tomatoes/Metacritic scores, and streaming providers. |
| **2** | **Exclude Filters & Weighted Random** | Support excluding genres/types (e.g., "not Horror"), runtime bounds, and optional weighted random selection (e.g., favor higher ratings or older entries). |
| **3** | **Watchlist Set Operations** | Union, intersection, and difference across watchlists. |
| **4** | **"Watch Together" Pick Mode** | Pick a random title from a watchlist that none of a selected group of users have seen yet. |
| **5** | **Multi-user Auth & Ownership** | User authentication and `owner_user_id` scoping for watchlists/profiles (essential once hosted publicly). |
| **6** | **Multi-language UI (i18n)** | English, Catalan, and Spanish translations in the React frontend. |

# Target Architecture & Design Specification

This document specifies the **proposed target design** for IMDBSelector, comparing against the legacy design in [current-architecture.md](current-architecture.md) so all architectural, schema, and API decisions can be reviewed before implementation.

---

## 1. Chosen Tech Stack

| Layer | Technology | Rationale |
| :--- | :--- | :--- |
| **Backend Language** | **Java 25** | Modern syntax (`record`, `sealed`, pattern matching, `Stream`s). |
| **Web / HTTP** | **Javalin** | Lightweight, explicit routing; zero DI magic. |
| **Persistence / SQL** | **jOOQ + Flyway + PostgreSQL** | Type-safe SQL, versioned migrations, zero ORM/JPA leakage into Domain. |
| **Build & Quality** | **Gradle (Kotlin DSL)**, **JUnit 5**, **Testcontainers**, **ArchUnit** | Single-command verification + automated hexagonal boundary enforcement. |
| **Frontend** | **React + TypeScript (Vite)** | Decoupled SPA communicating with the backend via stateless REST/JSON. |
| **Local Infra** | **Docker Compose** | Reproducible PostgreSQL instance for local dev and testing. |

---

## 2. Hexagonal Architecture (Ports & Adapters)

Unlike the legacy app (where `Domain` instantiated `MainView` and `UserGateway`), **all dependencies point inward** toward `domain`. Wiring happens in a single `AppAssembler` (`infrastructure/config`) using plain Java constructors.

```mermaid
flowchart TB
    subgraph Frontend["Frontend (React + TypeScript)"]
        UI["React SPA"]
    end

    subgraph Infrastructure["Infrastructure (Adapters & Wiring)"]
        Javalin["Inbound: Javalin REST Controllers\n(/api/users, /api/watchlists, ...)"]
        JooqRepo["Outbound: jOOQ Repositories\n(Postgres)"]
        CsvAdapter["Outbound: OpenCSV Parser Adapter"]
        JsonMigrator["Inbound/CLI: Legacy JSON Migration Tool"]
        Assembler["AppAssembler (Manual DI)"]
    end

    subgraph Application["Application (Use Cases & Ports)"]
        InPorts["Inbound Ports\n(UseCase interfaces)"]
        UseCases["Use Case Implementations"]
        OutPorts["Outbound Ports\n(UserRepository, WatchlistRepository,\nMediaItemRepository, CsvParserPort)"]
    end

    subgraph Domain["Domain (Pure Java 25 — Zero Framework Imports)"]
        Aggregates["Aggregates: User, Watchlist"]
        Catalog["Entity: MediaItem"]
        VOs["Value Objects: ImdbId, Rating,\nReleaseYear, Genre, MediaType,\nFilterCriteria"]
        DomainSvc["Domain Services: RandomMediaSelector,\nWatchlistFilter"]
    end

    DB[("PostgreSQL")]

    UI -->|HTTP / JSON| Javalin
    Javalin --> InPorts
    JsonMigrator --> InPorts
    UseCases -.->|implements| InPorts
    UseCases --> OutPorts
    UseCases --> Domain
    JooqRepo -.->|implements| OutPorts
    CsvAdapter -.->|implements| OutPorts
    JooqRepo --> DB
    Assembler -.->|wires| Infrastructure
    Assembler -.->|wires| Application
```

---

## 3. Domain Model (DDD) vs. Legacy

### Key Changes from Legacy
1. **Normalized `MediaItem` catalog**: In legacy JSONs, full `Movie` objects were duplicated inside every `Watchlist` and every `User.seenMovies`. Now `MediaItem` is a canonical entity keyed by `ImdbId` (`tt...`), shared across watchlists and users—making future IMDB/TMDB enrichment clean.
2. **Rich Value Objects**: `Movie.year` (`String`), `Movie.rating` (`String`), and `Filter.checkedGenres` (`boolean[]`) become validated Java 25 `record`s (`ImdbId`, `ReleaseYear`, `Rating`, `Set<Genre>`, `Set<MediaType>`).
3. **Stateless Backend**: `ModelImplementation` no longer holds "current user/watchlist/movie" in memory. The React frontend holds the active selection and passes IDs/filters in REST requests.

```mermaid
classDiagram
    class User {
        <<Aggregate Root>>
        +UserId id
        +String name
        +Set~SeenMedia~ seenMedia
        +ImdbId lastGeneratedImdbId
        +Instant lastGeneratedAt
        +markAsSeen(ImdbId, Instant)
        +unmarkAsSeen(ImdbId)
        +recordGenerated(ImdbId, Instant)
        +hasSeen(ImdbId) boolean
    }

    class Watchlist {
        <<Aggregate Root>>
        +WatchlistId id
        +String name
        +List~ImdbId~ mediaItemIds
        +replaceItems(List~ImdbId~)
        +filter(FilterCriteria, Map~ImdbId, MediaItem~) List~MediaItem~
    }

    class MediaItem {
        <<Entity>>
        +ImdbId imdbId
        +String title
        +String director
        +ReleaseYear releaseYear
        +Rating rating
        +MediaType mediaType
        +String imdbUrl
        +Set~Genre~ genres
        +MetadataSource source
    }

    class FilterCriteria {
        <<Value Object>>
        +Rating minRating
        +Rating maxRating
        +ReleaseYear minYear
        +ReleaseYear maxYear
        +Set~Genre~ genres
        +Set~MediaType~ mediaTypes
        +List~String~ keywords
        +boolean excludeSeen
    }

    User "1" --> "*" MediaItem : seen / lastGenerated (by ImdbId)
    Watchlist "1" --> "*" MediaItem : contains (by ImdbId)
    Watchlist ..> FilterCriteria : filtered by
```

---

## 4. PostgreSQL Schema (ER Diagram)

Designed for **v1 parity** while remaining **extension-ready** (nullable `owner_user_id` or join tables can be added later for multi-user/auth, and `media_items` can gain `poster_url`, `runtime_minutes`, or `rotten_tomatoes_score` without touching `users` or `watchlists`).

```mermaid
erDiagram
    USERS {
        uuid id PK
        text name UK
        text last_generated_imdb_id FK "nullable"
        timestamptz last_generated_at "nullable"
        timestamptz created_at
    }

    MEDIA_ITEMS {
        text imdb_id PK "e.g. tt0111161"
        text title
        text director
        int release_year "nullable"
        numeric rating "nullable, 0.0-10.0"
        text media_type
        text imdb_url
        text source "CSV | API"
        timestamptz updated_at
    }

    MEDIA_ITEM_GENRES {
        text imdb_id PK, FK
        text genre PK
    }

    WATCHLISTS {
        uuid id PK
        text name UK
        timestamptz created_at
        timestamptz updated_at
    }

    WATCHLIST_ITEMS {
        uuid watchlist_id PK, FK
        text imdb_id PK, FK
        timestamptz added_at
    }

    USER_SEEN_MEDIA {
        uuid user_id PK, FK
        text imdb_id PK, FK
        timestamptz seen_at
    }

    MEDIA_ITEMS ||--o{ MEDIA_ITEM_GENRES : has
    WATCHLISTS ||--o{ WATCHLIST_ITEMS : contains
    MEDIA_ITEMS ||--o{ WATCHLIST_ITEMS : referenced_in
    USERS ||--o{ USER_SEEN_MEDIA : tracks
    MEDIA_ITEMS ||--o{ USER_SEEN_MEDIA : marked_by
    MEDIA_ITEMS ||--o{ USERS : last_generated
```

---

## 5. Proposed Stateless REST API Contract

| Resource | Method & Path | Purpose |
| :--- | :--- | :--- |
| **Users** | `GET /api/users` | List all user profiles |
| | `POST /api/users` | Create a user profile |
| | `GET /api/users/{userId}` | Get profile + `lastGenerated` (for "mark as seen?" prompt) |
| | `DELETE /api/users/{userId}` | Delete a user profile |
| **Seen Media** | `GET /api/users/{userId}/seen` | List seen `imdbId`s / media items for a user |
| | `POST /api/users/{userId}/seen/{imdbId}` | Mark a media item as seen |
| | `DELETE /api/users/{userId}/seen/{imdbId}` | Remove a media item from seen |
| **Watchlists** | `GET /api/watchlists` | List all watchlists |
| | `POST /api/watchlists` | Create watchlist from uploaded IMDB CSV |
| | `PUT /api/watchlists/{watchlistId}/import` | Replace/update watchlist items from new CSV |
| | `GET /api/watchlists/{watchlistId}/items` | View media items in watchlist (supports filter query params + `userId` for seen status) |
| | `DELETE /api/watchlists/{watchlistId}` | Delete a watchlist |
| **Selection** | `POST /api/watchlists/{watchlistId}/select` | Pick a random media item matching `FilterCriteria`, optionally excluding `userId`'s seen media and updating `lastGenerated` |

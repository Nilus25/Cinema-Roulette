CREATE TABLE media_items (
    imdb_id       VARCHAR(32) PRIMARY KEY,
    title         TEXT NOT NULL,
    director      TEXT NOT NULL DEFAULT '',
    release_year  INT,
    rating        NUMERIC(3, 1) CHECK (rating IS NULL OR (rating >= 0.0 AND rating <= 10.0)),
    media_type    VARCHAR(64) NOT NULL,
    imdb_url      TEXT NOT NULL,
    source        VARCHAR(32) NOT NULL DEFAULT 'CSV',
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE media_item_genres (
    imdb_id VARCHAR(32) NOT NULL REFERENCES media_items(imdb_id) ON DELETE CASCADE,
    genre   VARCHAR(64) NOT NULL,
    PRIMARY KEY (imdb_id, genre)
);

CREATE TABLE users (
    id                      UUID PRIMARY KEY,
    name                    VARCHAR(255) NOT NULL UNIQUE,
    last_generated_imdb_id  VARCHAR(32) REFERENCES media_items(imdb_id) ON DELETE SET NULL,
    last_generated_at       TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE watchlists (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE watchlist_items (
    watchlist_id UUID NOT NULL REFERENCES watchlists(id) ON DELETE CASCADE,
    imdb_id      VARCHAR(32) NOT NULL REFERENCES media_items(imdb_id) ON DELETE CASCADE,
    added_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (watchlist_id, imdb_id)
);

CREATE TABLE user_seen_media (
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    imdb_id VARCHAR(32) NOT NULL REFERENCES media_items(imdb_id) ON DELETE CASCADE,
    seen_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, imdb_id)
);

CREATE INDEX idx_watchlist_items_imdb_id ON watchlist_items(imdb_id);
CREATE INDEX idx_user_seen_media_imdb_id ON user_seen_media(imdb_id);

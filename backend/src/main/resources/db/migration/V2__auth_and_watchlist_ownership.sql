ALTER TABLE users
    ADD COLUMN password_hash TEXT;

ALTER TABLE watchlists
    ADD COLUMN owner_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    ADD COLUMN visibility VARCHAR(16) NOT NULL DEFAULT 'PRIVATE',
    ADD COLUMN share_code VARCHAR(32);

-- Replace global UNIQUE(name) with per-owner uniqueness
ALTER TABLE watchlists DROP CONSTRAINT IF EXISTS watchlists_name_key;
ALTER TABLE watchlists DROP CONSTRAINT IF EXISTS constraint_fa_name;
CREATE UNIQUE INDEX idx_watchlists_owner_name ON watchlists(owner_user_id, name);
CREATE UNIQUE INDEX idx_watchlists_share_code ON watchlists(share_code);

CREATE TABLE watchlist_subscriptions (
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    watchlist_id  UUID NOT NULL REFERENCES watchlists(id) ON DELETE CASCADE,
    subscribed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, watchlist_id)
);

CREATE INDEX idx_watchlist_subscriptions_watchlist_id ON watchlist_subscriptions(watchlist_id);

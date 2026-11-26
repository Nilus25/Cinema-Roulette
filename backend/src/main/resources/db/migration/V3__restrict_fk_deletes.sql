ALTER TABLE watchlists DROP CONSTRAINT IF EXISTS watchlists_owner_user_id_fkey;
ALTER TABLE watchlists ADD CONSTRAINT watchlists_owner_user_id_fkey FOREIGN KEY (owner_user_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE watchlist_subscriptions DROP CONSTRAINT IF EXISTS watchlist_subscriptions_user_id_fkey;
ALTER TABLE watchlist_subscriptions ADD CONSTRAINT watchlist_subscriptions_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

ALTER TABLE watchlist_subscriptions DROP CONSTRAINT IF EXISTS watchlist_subscriptions_watchlist_id_fkey;
ALTER TABLE watchlist_subscriptions ADD CONSTRAINT watchlist_subscriptions_watchlist_id_fkey FOREIGN KEY (watchlist_id) REFERENCES watchlists(id) ON DELETE RESTRICT;

ALTER TABLE watchlist_items DROP CONSTRAINT IF EXISTS watchlist_items_watchlist_id_fkey;
ALTER TABLE watchlist_items ADD CONSTRAINT watchlist_items_watchlist_id_fkey FOREIGN KEY (watchlist_id) REFERENCES watchlists(id) ON DELETE RESTRICT;

ALTER TABLE user_seen_media DROP CONSTRAINT IF EXISTS user_seen_media_user_id_fkey;
ALTER TABLE user_seen_media ADD CONSTRAINT user_seen_media_user_id_fkey FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT;

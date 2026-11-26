package com.imdbselector.infrastructure.adapter.out.persistence;

import com.imdbselector.application.port.out.WatchlistSubscriptionRepository;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;

import static com.imdbselector.infrastructure.jooq.Tables.WATCHLIST_SUBSCRIPTIONS;

public final class JooqWatchlistSubscriptionRepository implements WatchlistSubscriptionRepository {
    private final DSLContext dsl;

    public JooqWatchlistSubscriptionRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl);
    }

    @Override
    public void subscribe(UserId userId, WatchlistId watchlistId) {
        dsl.insertInto(WATCHLIST_SUBSCRIPTIONS)
                .set(WATCHLIST_SUBSCRIPTIONS.USER_ID, userId.value())
                .set(WATCHLIST_SUBSCRIPTIONS.WATCHLIST_ID, watchlistId.value())
                .set(WATCHLIST_SUBSCRIPTIONS.SUBSCRIBED_AT, OffsetDateTime.now(ZoneOffset.UTC))
                .onConflictDoNothing()
                .execute();
    }

    @Override
    public void unsubscribe(UserId userId, WatchlistId watchlistId) {
        dsl.deleteFrom(WATCHLIST_SUBSCRIPTIONS)
                .where(WATCHLIST_SUBSCRIPTIONS.USER_ID.eq(userId.value())
                        .and(WATCHLIST_SUBSCRIPTIONS.WATCHLIST_ID.eq(watchlistId.value())))
                .execute();
    }

    @Override
    public boolean isSubscribed(UserId userId, WatchlistId watchlistId) {
        return dsl.fetchExists(
                dsl.selectOne()
                        .from(WATCHLIST_SUBSCRIPTIONS)
                        .where(WATCHLIST_SUBSCRIPTIONS.USER_ID.eq(userId.value())
                                .and(WATCHLIST_SUBSCRIPTIONS.WATCHLIST_ID.eq(watchlistId.value())))
        );
    }

    @Override
    public List<WatchlistId> findSubscribedWatchlistIds(UserId userId) {
        return dsl.select(WATCHLIST_SUBSCRIPTIONS.WATCHLIST_ID)
                .from(WATCHLIST_SUBSCRIPTIONS)
                .where(WATCHLIST_SUBSCRIPTIONS.USER_ID.eq(userId.value()))
                .fetch(r -> new WatchlistId(r.value1()));
    }
}

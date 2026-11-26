package com.imdbselector.infrastructure.adapter.out.persistence;

import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.WatchlistId;
import org.jooq.DSLContext;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.imdbselector.infrastructure.jooq.Tables.WATCHLISTS;
import static com.imdbselector.infrastructure.jooq.Tables.WATCHLIST_ITEMS;

public final class JooqWatchlistRepository implements WatchlistRepository {
    private final DSLContext dsl;

    public JooqWatchlistRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl);
    }

    @Override
    public List<Watchlist> findAll() {
        var rows = dsl.selectFrom(WATCHLISTS).orderBy(WATCHLISTS.NAME.asc()).fetch();
        Map<UUID, List<ImdbId>> itemsByWatchlist = dsl.selectFrom(WATCHLIST_ITEMS)
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.get(WATCHLIST_ITEMS.WATCHLIST_ID),
                        Collectors.mapping(r -> new ImdbId(r.get(WATCHLIST_ITEMS.IMDB_ID)), Collectors.toList())
                ));

        return rows.map(r -> new Watchlist(
                new WatchlistId(r.get(WATCHLISTS.ID)),
                r.get(WATCHLISTS.NAME),
                itemsByWatchlist.getOrDefault(r.get(WATCHLISTS.ID), List.of()),
                r.get(WATCHLISTS.CREATED_AT).toInstant(),
                r.get(WATCHLISTS.UPDATED_AT).toInstant()
        ));
    }

    @Override
    public Optional<Watchlist> findById(WatchlistId id) {
        return findOne(dsl.selectFrom(WATCHLISTS).where(WATCHLISTS.ID.eq(id.value())).fetchOne());
    }

    @Override
    public Optional<Watchlist> findByName(String name) {
        return findOne(dsl.selectFrom(WATCHLISTS).where(WATCHLISTS.NAME.eq(name)).fetchOne());
    }

    private Optional<Watchlist> findOne(com.imdbselector.infrastructure.jooq.tables.records.WatchlistsRecord record) {
        if (record == null) {
            return Optional.empty();
        }
        List<ImdbId> ids = dsl.select(WATCHLIST_ITEMS.IMDB_ID)
                .from(WATCHLIST_ITEMS)
                .where(WATCHLIST_ITEMS.WATCHLIST_ID.eq(record.getId()))
                .fetch(r -> new ImdbId(r.value1()));

        return Optional.of(new Watchlist(
                new WatchlistId(record.getId()),
                record.getName(),
                ids,
                record.getCreatedAt().toInstant(),
                record.getUpdatedAt().toInstant()
        ));
    }

    @Override
    public Watchlist save(Watchlist watchlist) {
        dsl.transaction(cfg -> {
            DSLContext tx = cfg.dsl();
            var created = watchlist.createdAt().atOffset(ZoneOffset.UTC);
            var updated = watchlist.updatedAt().atOffset(ZoneOffset.UTC);

            tx.insertInto(WATCHLISTS)
                    .set(WATCHLISTS.ID, watchlist.id().value())
                    .set(WATCHLISTS.NAME, watchlist.name())
                    .set(WATCHLISTS.CREATED_AT, created)
                    .set(WATCHLISTS.UPDATED_AT, updated)
                    .onConflict(WATCHLISTS.ID)
                    .doUpdate()
                    .set(WATCHLISTS.NAME, watchlist.name())
                    .set(WATCHLISTS.UPDATED_AT, updated)
                    .execute();

            tx.deleteFrom(WATCHLIST_ITEMS)
                    .where(WATCHLIST_ITEMS.WATCHLIST_ID.eq(watchlist.id().value()))
                    .execute();

            for (ImdbId imdbId : watchlist.mediaItemIds()) {
                tx.insertInto(WATCHLIST_ITEMS)
                        .set(WATCHLIST_ITEMS.WATCHLIST_ID, watchlist.id().value())
                        .set(WATCHLIST_ITEMS.IMDB_ID, imdbId.value())
                        .set(WATCHLIST_ITEMS.ADDED_AT, updated)
                        .onConflictDoNothing()
                        .execute();
            }
        });
        return watchlist;
    }

    @Override
    public void deleteById(WatchlistId id) {
        dsl.deleteFrom(WATCHLISTS)
                .where(WATCHLISTS.ID.eq(id.value()))
                .execute();
    }
}

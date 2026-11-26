package com.imdbselector.infrastructure.adapter.out.persistence;

import com.imdbselector.application.port.out.WatchlistRepository;
import com.imdbselector.domain.model.Watchlist;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.domain.valueobject.WatchlistId;
import com.imdbselector.domain.valueobject.WatchlistVisibility;
import com.imdbselector.infrastructure.jooq.tables.records.WatchlistsRecord;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
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
    public List<Watchlist> findByOwner(UserId ownerUserId) {
        return mapRows(dsl.selectFrom(WATCHLISTS)
                .where(WATCHLISTS.OWNER_USER_ID.eq(ownerUserId.value()))
                .orderBy(WATCHLISTS.NAME.asc())
                .fetch());
    }

    @Override
    public List<Watchlist> findByIds(Collection<WatchlistId> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<UUID> raw = ids.stream().map(WatchlistId::value).toList();
        return mapRows(dsl.selectFrom(WATCHLISTS)
                .where(WATCHLISTS.ID.in(raw))
                .orderBy(WATCHLISTS.NAME.asc())
                .fetch());
    }

    @Override
    public List<Watchlist> findPublicWatchlists() {
        return mapRows(dsl.selectFrom(WATCHLISTS)
                .where(WATCHLISTS.VISIBILITY.eq(WatchlistVisibility.PUBLIC.name()))
                .orderBy(WATCHLISTS.NAME.asc())
                .fetch());
    }

    @Override
    public Optional<Watchlist> findById(WatchlistId id) {
        return findOne(dsl.selectFrom(WATCHLISTS).where(WATCHLISTS.ID.eq(id.value())).fetchOne());
    }

    @Override
    public Optional<Watchlist> findByOwnerAndName(UserId ownerUserId, String name) {
        return findOne(dsl.selectFrom(WATCHLISTS)
                .where(WATCHLISTS.OWNER_USER_ID.eq(ownerUserId.value()).and(WATCHLISTS.NAME.eq(name)))
                .fetchOne());
    }

    @Override
    public Optional<Watchlist> findByShareCode(String shareCode) {
        return findOne(dsl.selectFrom(WATCHLISTS)
                .where(WATCHLISTS.SHARE_CODE.eq(shareCode))
                .fetchOne());
    }

    private List<Watchlist> mapRows(List<WatchlistsRecord> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = rows.stream().map(WatchlistsRecord::getId).toList();
        Map<UUID, List<ImdbId>> itemsByWatchlist = dsl.selectFrom(WATCHLIST_ITEMS)
                .where(WATCHLIST_ITEMS.WATCHLIST_ID.in(ids))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.get(WATCHLIST_ITEMS.WATCHLIST_ID),
                        Collectors.mapping(r -> new ImdbId(r.get(WATCHLIST_ITEMS.IMDB_ID)), Collectors.toList())
                ));

        return rows.stream()
                .map(r -> mapRecord(r, itemsByWatchlist.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    private Optional<Watchlist> findOne(WatchlistsRecord record) {
        if (record == null) {
            return Optional.empty();
        }
        List<ImdbId> ids = dsl.select(WATCHLIST_ITEMS.IMDB_ID)
                .from(WATCHLIST_ITEMS)
                .where(WATCHLIST_ITEMS.WATCHLIST_ID.eq(record.getId()))
                .fetch(r -> new ImdbId(r.value1()));
        return Optional.of(mapRecord(record, ids));
    }

    private Watchlist mapRecord(WatchlistsRecord r, List<ImdbId> ids) {
        UUID ownerUuid = r.getOwnerUserId();
        return new Watchlist(
                new WatchlistId(r.getId()),
                ownerUuid == null ? null : new UserId(ownerUuid),
                r.getName(),
                WatchlistVisibility.valueOf(r.getVisibility()),
                r.getShareCode(),
                ids,
                r.getCreatedAt().toInstant(),
                r.getUpdatedAt().toInstant()
        );
    }

    @Override
    public Watchlist save(Watchlist watchlist) {
        dsl.transaction(cfg -> {
            DSLContext tx = cfg.dsl();
            OffsetDateTime created = watchlist.createdAt().atOffset(ZoneOffset.UTC);
            OffsetDateTime updated = watchlist.updatedAt().atOffset(ZoneOffset.UTC);
            UUID ownerUuid = watchlist.ownerUserId() == null ? null : watchlist.ownerUserId().value();

            tx.insertInto(WATCHLISTS)
                    .set(WATCHLISTS.ID, watchlist.id().value())
                    .set(WATCHLISTS.OWNER_USER_ID, ownerUuid)
                    .set(WATCHLISTS.NAME, watchlist.name())
                    .set(WATCHLISTS.VISIBILITY, watchlist.visibility().name())
                    .set(WATCHLISTS.SHARE_CODE, watchlist.shareCode())
                    .set(WATCHLISTS.CREATED_AT, created)
                    .set(WATCHLISTS.UPDATED_AT, updated)
                    .onConflict(WATCHLISTS.ID)
                    .doUpdate()
                    .set(WATCHLISTS.NAME, watchlist.name())
                    .set(WATCHLISTS.VISIBILITY, watchlist.visibility().name())
                    .set(WATCHLISTS.SHARE_CODE, watchlist.shareCode())
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
        dsl.deleteFrom(WATCHLIST_ITEMS)
                .where(WATCHLIST_ITEMS.WATCHLIST_ID.eq(id.value()))
                .execute();
        dsl.deleteFrom(WATCHLISTS)
                .where(WATCHLISTS.ID.eq(id.value()))
                .execute();
    }
}

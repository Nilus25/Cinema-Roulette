package com.imdbselector.infrastructure.adapter.out.persistence;

import com.imdbselector.application.port.out.MediaItemRepository;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import org.jooq.DSLContext;
import org.jooq.Record;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.imdbselector.infrastructure.jooq.Tables.MEDIA_ITEMS;
import static com.imdbselector.infrastructure.jooq.Tables.MEDIA_ITEM_GENRES;

public final class JooqMediaItemRepository implements MediaItemRepository {
    private final DSLContext dsl;

    public JooqMediaItemRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl);
    }

    @Override
    public void upsertAll(List<MediaItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        dsl.transaction(cfg -> {
            DSLContext tx = cfg.dsl();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            for (MediaItem item : items) {
                tx.insertInto(MEDIA_ITEMS)
                        .set(MEDIA_ITEMS.IMDB_ID, item.imdbId().value())
                        .set(MEDIA_ITEMS.TITLE, item.title())
                        .set(MEDIA_ITEMS.DIRECTOR, item.director())
                        .set(MEDIA_ITEMS.RELEASE_YEAR, item.releaseYear() == null ? null : item.releaseYear().value())
                        .set(MEDIA_ITEMS.RATING, item.rating() == null ? null : item.rating().value())
                        .set(MEDIA_ITEMS.MEDIA_TYPE, item.mediaType().label())
                        .set(MEDIA_ITEMS.IMDB_URL, item.imdbUrl())
                        .set(MEDIA_ITEMS.SOURCE, item.source().name())
                        .set(MEDIA_ITEMS.UPDATED_AT, now)
                        .onConflict(MEDIA_ITEMS.IMDB_ID)
                        .doUpdate()
                        .set(MEDIA_ITEMS.TITLE, item.title())
                        .set(MEDIA_ITEMS.DIRECTOR, item.director())
                        .set(MEDIA_ITEMS.RELEASE_YEAR, item.releaseYear() == null ? null : item.releaseYear().value())
                        .set(MEDIA_ITEMS.RATING, item.rating() == null ? null : item.rating().value())
                        .set(MEDIA_ITEMS.MEDIA_TYPE, item.mediaType().label())
                        .set(MEDIA_ITEMS.IMDB_URL, item.imdbUrl())
                        .set(MEDIA_ITEMS.SOURCE, item.source().name())
                        .set(MEDIA_ITEMS.UPDATED_AT, now)
                        .execute();

                tx.deleteFrom(MEDIA_ITEM_GENRES)
                        .where(MEDIA_ITEM_GENRES.IMDB_ID.eq(item.imdbId().value()))
                        .execute();

                for (Genre genre : item.genres()) {
                    tx.insertInto(MEDIA_ITEM_GENRES)
                            .set(MEDIA_ITEM_GENRES.IMDB_ID, item.imdbId().value())
                            .set(MEDIA_ITEM_GENRES.GENRE, genre.label())
                            .onConflictDoNothing()
                            .execute();
                }
            }
        });
    }

    @Override
    public Optional<MediaItem> findById(ImdbId imdbId) {
        return findByIds(List.of(imdbId)).stream().findFirst();
    }

    @Override
    public List<MediaItem> findByIds(Collection<ImdbId> imdbIds) {
        if (imdbIds == null || imdbIds.isEmpty()) {
            return List.of();
        }
        List<String> rawIds = imdbIds.stream().map(ImdbId::value).toList();

        Map<String, Set<Genre>> genresByImdbId = dsl.selectFrom(MEDIA_ITEM_GENRES)
                .where(MEDIA_ITEM_GENRES.IMDB_ID.in(rawIds))
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.get(MEDIA_ITEM_GENRES.IMDB_ID),
                        Collectors.mapping(r -> Genre.fromLabel(r.get(MEDIA_ITEM_GENRES.GENRE)), Collectors.toSet())
                ));

        return dsl.selectFrom(MEDIA_ITEMS)
                .where(MEDIA_ITEMS.IMDB_ID.in(rawIds))
                .fetch()
                .map(r -> mapRecord(r, genresByImdbId.getOrDefault(r.get(MEDIA_ITEMS.IMDB_ID), Set.of())));
    }

    private MediaItem mapRecord(Record r, Set<Genre> genres) {
        Integer year = r.get(MEDIA_ITEMS.RELEASE_YEAR);
        var rating = r.get(MEDIA_ITEMS.RATING);
        return new MediaItem(
                new ImdbId(r.get(MEDIA_ITEMS.IMDB_ID)),
                r.get(MEDIA_ITEMS.TITLE),
                r.get(MEDIA_ITEMS.DIRECTOR),
                year == null ? null : new ReleaseYear(year),
                rating == null ? null : new Rating(rating),
                MediaType.fromLabel(r.get(MEDIA_ITEMS.MEDIA_TYPE)),
                r.get(MEDIA_ITEMS.IMDB_URL),
                genres,
                MetadataSource.valueOf(r.get(MEDIA_ITEMS.SOURCE))
        );
    }
}

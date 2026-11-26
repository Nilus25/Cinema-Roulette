package com.imdbselector.infrastructure.adapter.out.persistence;

import com.imdbselector.application.port.out.UserRepository;
import com.imdbselector.domain.model.User;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;
import com.imdbselector.infrastructure.jooq.tables.records.UsersRecord;
import org.jooq.DSLContext;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.imdbselector.infrastructure.jooq.Tables.USERS;
import static com.imdbselector.infrastructure.jooq.Tables.USER_SEEN_MEDIA;

public final class JooqUserRepository implements UserRepository {
    private final DSLContext dsl;

    public JooqUserRepository(DSLContext dsl) {
        this.dsl = Objects.requireNonNull(dsl);
    }

    @Override
    public List<User> findAll() {
        var rows = dsl.selectFrom(USERS).orderBy(USERS.NAME.asc()).fetch();
        Map<UUID, Set<ImdbId>> seenByUser = dsl.selectFrom(USER_SEEN_MEDIA)
                .fetch()
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.get(USER_SEEN_MEDIA.USER_ID),
                        Collectors.mapping(r -> new ImdbId(r.get(USER_SEEN_MEDIA.IMDB_ID)), Collectors.toSet())
                ));

        return rows.map(r -> mapUser(r, seenByUser.getOrDefault(r.getId(), Set.of())));
    }

    @Override
    public Optional<User> findById(UserId id) {
        return findOne(dsl.selectFrom(USERS).where(USERS.ID.eq(id.value())).fetchOne());
    }

    @Override
    public Optional<User> findByName(String name) {
        return findOne(dsl.selectFrom(USERS).where(USERS.NAME.eq(name)).fetchOne());
    }

    private Optional<User> findOne(UsersRecord record) {
        if (record == null) {
            return Optional.empty();
        }
        Set<ImdbId> seen = dsl.select(USER_SEEN_MEDIA.IMDB_ID)
                .from(USER_SEEN_MEDIA)
                .where(USER_SEEN_MEDIA.USER_ID.eq(record.getId()))
                .fetchSet(r -> new ImdbId(r.value1()));

        return Optional.of(mapUser(record, seen));
    }

    private User mapUser(UsersRecord record, Set<ImdbId> seen) {
        String lastId = record.getLastGeneratedImdbId();
        var lastAt = record.getLastGeneratedAt();
        return new User(
                new UserId(record.getId()),
                record.getName(),
                record.getPasswordHash(),
                seen,
                lastId == null ? null : new ImdbId(lastId),
                lastAt == null ? null : lastAt.toInstant(),
                record.getCreatedAt().toInstant()
        );
    }

    @Override
    public User save(User user) {
        dsl.transaction(cfg -> {
            DSLContext tx = cfg.dsl();
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime created = user.createdAt().atOffset(ZoneOffset.UTC);
            OffsetDateTime lastGenAt = user.lastGeneratedAt() == null ? null : user.lastGeneratedAt().atOffset(ZoneOffset.UTC);
            String lastGenId = user.lastGeneratedImdbId() == null ? null : user.lastGeneratedImdbId().value();

            tx.insertInto(USERS)
                    .set(USERS.ID, user.id().value())
                    .set(USERS.NAME, user.name())
                    .set(USERS.PASSWORD_HASH, user.passwordHash())
                    .set(USERS.LAST_GENERATED_IMDB_ID, lastGenId)
                    .set(USERS.LAST_GENERATED_AT, lastGenAt)
                    .set(USERS.CREATED_AT, created)
                    .onConflict(USERS.ID)
                    .doUpdate()
                    .set(USERS.NAME, user.name())
                    .set(USERS.PASSWORD_HASH, user.passwordHash())
                    .set(USERS.LAST_GENERATED_IMDB_ID, lastGenId)
                    .set(USERS.LAST_GENERATED_AT, lastGenAt)
                    .execute();

            tx.deleteFrom(USER_SEEN_MEDIA)
                    .where(USER_SEEN_MEDIA.USER_ID.eq(user.id().value()))
                    .execute();

            for (ImdbId imdbId : user.seenMediaIds()) {
                tx.insertInto(USER_SEEN_MEDIA)
                        .set(USER_SEEN_MEDIA.USER_ID, user.id().value())
                        .set(USER_SEEN_MEDIA.IMDB_ID, imdbId.value())
                        .set(USER_SEEN_MEDIA.SEEN_AT, now)
                        .onConflictDoNothing()
                        .execute();
            }
        });
        return user;
    }

    @Override
    public void deleteById(UserId id) {
        dsl.deleteFrom(USERS)
                .where(USERS.ID.eq(id.value()))
                .execute();
    }
}

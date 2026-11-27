package com.imdbselector.domain.model;

import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.UserId;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Aggregate root representing a viewer account and their seen-media history.
 */
public final class User {
    private final UserId id;
    private final String name;
    private String passwordHash;
    private final Set<ImdbId> seenMediaIds;
    private ImdbId lastGeneratedImdbId;
    private Instant lastGeneratedAt;
    private final Instant createdAt;

    public User(
            UserId id,
            String name,
            String passwordHash,
            Set<ImdbId> seenMediaIds,
            ImdbId lastGeneratedImdbId,
            Instant lastGeneratedAt,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        this.name = name.trim();
        this.passwordHash = passwordHash;
        this.seenMediaIds = seenMediaIds == null ? new LinkedHashSet<>() : new LinkedHashSet<>(seenMediaIds);
        this.lastGeneratedImdbId = lastGeneratedImdbId;
        this.lastGeneratedAt = lastGeneratedAt;
        this.createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public static User create(String name) {
        return new User(UserId.random(), name, null, Set.of(), null, null, Instant.now());
    }

    public static User register(String name, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        return new User(UserId.random(), name, passwordHash, Set.of(), null, null, Instant.now());
    }

    public void markAsSeen(ImdbId imdbId) {
        Objects.requireNonNull(imdbId, "imdbId must not be null");
        seenMediaIds.add(imdbId);
    }

    public void unmarkAsSeen(ImdbId imdbId) {
        Objects.requireNonNull(imdbId, "imdbId must not be null");
        seenMediaIds.remove(imdbId);
    }

    public void recordGenerated(ImdbId imdbId, Instant generatedAt) {
        this.lastGeneratedImdbId = Objects.requireNonNull(imdbId, "imdbId must not be null");
        this.lastGeneratedAt = Objects.requireNonNull(generatedAt, "generatedAt must not be null");
    }

    public void changePasswordHash(String newPasswordHash) {
        if (newPasswordHash == null || newPasswordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        this.passwordHash = newPasswordHash;
    }

    public boolean hasSeen(ImdbId imdbId) {
        return seenMediaIds.contains(imdbId);
    }

    public boolean isLastGeneratedUnseen() {
        return lastGeneratedImdbId != null && !seenMediaIds.contains(lastGeneratedImdbId);
    }

    public UserId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public Set<ImdbId> seenMediaIds() {
        return Collections.unmodifiableSet(seenMediaIds);
    }

    public ImdbId lastGeneratedImdbId() {
        return lastGeneratedImdbId;
    }

    public Instant lastGeneratedAt() {
        return lastGeneratedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }
}

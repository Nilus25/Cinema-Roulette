package com.imdbselector.domain.service;

import com.imdbselector.domain.valueobject.ImdbId;

import java.util.Set;

/**
 * Strategy for narrowing candidate MediaItems prior to random selection.
 */
public sealed interface SelectionMode permits SelectionMode.Random, SelectionMode.NotSeen {

    record Random() implements SelectionMode {
    }

    record NotSeen(Set<ImdbId> seenMediaIds) implements SelectionMode {
        public NotSeen {
            seenMediaIds = seenMediaIds == null ? Set.of() : Set.copyOf(seenMediaIds);
        }
    }
}

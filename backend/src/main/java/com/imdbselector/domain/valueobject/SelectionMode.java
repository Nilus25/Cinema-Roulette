package com.imdbselector.domain.valueobject;

import java.util.Set;

/**
 * Strategy for narrowing candidate MediaItems prior to roulette selection.
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

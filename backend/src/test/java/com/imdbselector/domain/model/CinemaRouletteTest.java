package com.imdbselector.domain.model;

import com.imdbselector.domain.exception.NoMatchingMediaException;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import com.imdbselector.domain.valueobject.SelectionMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CinemaRouletteTest {

    private final MediaItem seen = new MediaItem(
            new ImdbId("tt0111161"), "The Shawshank Redemption", "Frank Darabont",
            new ReleaseYear(1994), Rating.of(9.3), MediaType.MOVIE, null,
            Set.of(Genre.DRAMA), MetadataSource.CSV);

    private final MediaItem unseen = new MediaItem(
            new ImdbId("tt0068646"), "The Godfather", "Francis Ford Coppola",
            new ReleaseYear(1972), Rating.of(9.2), MediaType.MOVIE, null,
            Set.of(Genre.CRIME, Genre.DRAMA), MetadataSource.CSV);

    @Test
    void notSeenModeExcludesSeenItems() {
        CinemaRoulette roulette = new CinemaRoulette(new Random(42));
        MediaItem picked = roulette.spin(List.of(seen, unseen), new SelectionMode.NotSeen(Set.of(seen.imdbId())));
        assertThat(picked).isEqualTo(unseen);
    }

    @Test
    void throwsWhenAllCandidatesAlreadySeen() {
        CinemaRoulette roulette = new CinemaRoulette();
        assertThatThrownBy(() -> roulette.spin(List.of(seen), new SelectionMode.NotSeen(Set.of(seen.imdbId()))))
                .isInstanceOf(NoMatchingMediaException.class);
    }
}

package com.imdbselector.domain.service;

import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.valueobject.FilterCriteria;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WatchlistFilterServiceTest {

    private final WatchlistFilterService service = new WatchlistFilterService();

    private final MediaItem inception = new MediaItem(
            new ImdbId("tt1375666"), "Inception", "Christopher Nolan",
            new ReleaseYear(2010), Rating.of(8.8), MediaType.MOVIE, null,
            Set.of(Genre.ACTION, Genre.SCI_FI), MetadataSource.CSV);

    private final MediaItem dark = new MediaItem(
            new ImdbId("tt5753856"), "Dark", "Baran bo Odar",
            new ReleaseYear(2017), Rating.of(8.7), MediaType.TV_SERIES, null,
            Set.of(Genre.DRAMA, Genre.SCI_FI, Genre.MYSTERY), MetadataSource.CSV);

    private final MediaItem unratedShort = new MediaItem(
            new ImdbId("tt0000001"), "Unrated Experiment", "",
            null, null, MediaType.SHORT, null,
            Set.of(Genre.DRAMA), MetadataSource.CSV);

    @Test
    void filtersByGenreTypeYearRatingAndKeyword() {
        var criteria = new FilterCriteria(
                Rating.of(8.5), Rating.of(9.0),
                new ReleaseYear(2005), new ReleaseYear(2015),
                Set.of(Genre.SCI_FI), Set.of(MediaType.MOVIE),
                List.of("nolan"), false);

        var result = service.filter(List.of(inception, dark, unratedShort), criteria);

        assertThat(result).containsExactly(inception);
    }

    @Test
    void excludesUnratedItemsOnlyWhenRatingFilterIsSet() {
        var all = service.filter(List.of(inception, unratedShort), FilterCriteria.unfiltered());
        assertThat(all).containsExactly(inception, unratedShort);

        var ratedOnly = service.filter(
                List.of(inception, unratedShort),
                new FilterCriteria(Rating.of(0.0), null, null, null, null, null, null, false));
        assertThat(ratedOnly).containsExactly(inception);
    }
}

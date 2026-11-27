package com.imdbselector.domain.query;

import com.imdbselector.domain.exception.InvalidQueryException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.model.Watchlist;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QueryParserAndEvaluatorTest {

    private final MediaItem horrorDramaMovie = new MediaItem(
            new ImdbId("tt0081505"),
            "The Shining",
            "The Shining",
            "Stanley Kubrick",
            new ReleaseYear(1980),
            Rating.of(8.4),
            146,
            1150000,
            MediaType.MOVIE,
            null,
            Set.of(Genre.HORROR, Genre.DRAMA),
            MetadataSource.CSV
    );

    private final MediaItem horrorDramaRomanceMovie = new MediaItem(
            new ImdbId("tt0103874"),
            "Dracula",
            "Bram Stoker's Dracula",
            "Francis Ford Coppola",
            new ReleaseYear(1992),
            Rating.of(7.4),
            128,
            235000,
            MediaType.MOVIE,
            null,
            Set.of(Genre.HORROR, Genre.DRAMA, Genre.ROMANCE),
            MetadataSource.CSV
    );

    private final MediaItem tvSeries = new MediaItem(
            new ImdbId("tt5753856"),
            "Dark",
            "Dark",
            "Baran bo Odar",
            new ReleaseYear(2017),
            Rating.of(8.7),
            60,
            460000,
            MediaType.TV_SERIES,
            null,
            Set.of(Genre.SCI_FI, Genre.MYSTERY, Genre.DRAMA),
            MetadataSource.CSV
    );

    private final MediaItem foreignClassic = new MediaItem(
            new ImdbId("tt0060196"),
            "The Good, the Bad and the Ugly",
            "Il buono, il brutto, il cattivo",
            "Sergio Leone",
            new ReleaseYear(1966),
            Rating.of(8.8),
            178,
            907647,
            MediaType.MOVIE,
            null,
            Set.of(Genre.ADVENTURE, Genre.DRAMA, Genre.WESTERN),
            MetadataSource.CSV
    );

    private final MediaItem missingMetadataShort = new MediaItem(
            new ImdbId("tt9999999"),
            "Mystery Reel",
            null,
            "",
            null,
            null,
            null,
            null,
            MediaType.SHORT,
            null,
            Set.of(Genre.SHORT),
            MetadataSource.CSV
    );

    @Test
    void parsesAndEvaluatesComplexNestedBooleanQuery() {
        QueryExpression expr = QueryParser.parse(
                "(Movie AND (Horror AND Drama) AND NOT Romance) OR TV Series"
        );

        assertThat(expr.evaluate(horrorDramaMovie, false)).isTrue();
        assertThat(expr.evaluate(horrorDramaRomanceMovie, false)).isFalse();
        assertThat(expr.evaluate(tvSeries, false)).isTrue();
        assertThat(expr.evaluate(foreignClassic, false)).isFalse();
    }

    @Test
    void supportsAllNumericComparatorsAndNullSafety() {
        QueryExpression highRatedQuick = QueryParser.parse(
                "rating >= 8.4 AND runtime <= 150 AND year > 1979 AND votes != 0"
        );
        assertThat(highRatedQuick.evaluate(horrorDramaMovie, false)).isTrue();
        assertThat(highRatedQuick.evaluate(foreignClassic, false)).isFalse();
        assertThat(highRatedQuick.evaluate(missingMetadataShort, false)).isFalse();

        QueryExpression exactEq = QueryParser.parse("year == 1966 AND rating = 8.8 AND runtime < 180");
        assertThat(exactEq.evaluate(foreignClassic, false)).isTrue();
        assertThat(exactEq.evaluate(horrorDramaMovie, false)).isFalse();
    }

    @Test
    void matchesOriginalTitleInTextQueryAndBasicKeywordFilter() {
        QueryExpression titleExpr = QueryParser.parse("title:\"il buono\"");
        assertThat(titleExpr.evaluate(foreignClassic, false)).isTrue();
        assertThat(titleExpr.evaluate(horrorDramaMovie, false)).isFalse();

        FilterCriteria keywordCriteria = new FilterCriteria(
                null, null, null, null, Set.of(), Set.of(), List.of("cattivo"), false
        );
        assertThat(foreignClassic.matches(keywordCriteria)).isTrue();
    }

    @Test
    void supportsSeenAndUnseenPredicatesInQuery() {
        QueryExpression expr = QueryParser.parse("Movie AND unseen");
        Set<ImdbId> seenIds = Set.of(horrorDramaMovie.imdbId());

        List<MediaItem> result = Watchlist.filterItems(
                List.of(horrorDramaMovie, foreignClassic, tvSeries),
                new FilterCriteria(null, null, null, null, null, null, null, null, Set.of(), Set.of(), List.of(), false, "Movie AND unseen"),
                seenIds
        );

        assertThat(expr.evaluate(horrorDramaMovie, true)).isFalse();
        assertThat(expr.evaluate(foreignClassic, false)).isTrue();
        assertThat(result).containsExactly(foreignClassic);
    }

    @Test
    void optimizesCostBasedChildOrderingAndDoubleNegation() {
        QueryExpression optimized = QueryParser.parse("title:\"Shining\" AND Movie");
        assertThat(optimized).isInstanceOf(QueryExpression.And.class);
        QueryExpression.And andNode = (QueryExpression.And) optimized;
        assertThat(andNode.left()).isInstanceOf(QueryExpression.MediaTypeMatch.class);
        assertThat(andNode.right()).isInstanceOf(QueryExpression.TextMatch.class);

        QueryExpression doubleNeg = QueryParser.parse("NOT NOT Horror");
        assertThat(doubleNeg).isEqualTo(new QueryExpression.GenreMatch(Genre.HORROR));
    }

    @Test
    void rejectsMalformedOrAbusiveQueries() {
        assertThatThrownBy(() -> QueryParser.parse("(Movie AND Horror"))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining(")");

        assertThatThrownBy(() -> QueryParser.parse("Movie AND UnknownGenreXYZ"))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("UnknownGenreXYZ");

        assertThatThrownBy(() -> QueryParser.parse("(((((((((((Movie)))))))))))"))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("depth");

        String huge = "Movie OR ".repeat(60) + "Horror";
        assertThatThrownBy(() -> QueryParser.parse(huge))
                .isInstanceOf(InvalidQueryException.class)
                .hasMessageContaining("token limit");
    }

    @Test
    void rejectsInvertedNumericRangeBoundsInFilterCriteria() {
        assertThatThrownBy(() -> new FilterCriteria(
                null, null, null, null, 150, 90, null, null, Set.of(), Set.of(), List.of(), false, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minRuntime");

        assertThatThrownBy(() -> new FilterCriteria(
                null, null, null, null, null, null, 50000, 1000, Set.of(), Set.of(), List.of(), false, null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("minVotes");
    }
}

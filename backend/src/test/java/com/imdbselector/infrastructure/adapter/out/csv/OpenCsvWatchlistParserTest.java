package com.imdbselector.infrastructure.adapter.out.csv;

import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.Rating;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenCsvWatchlistParserTest {

    @Test
    void parsesHeaderMappedImdbCsvWithMissingOptionalValues() {
        String csv = """
                Position,Const,Created,Modified,Description,Title,URL,Title Type,IMDb Rating,Runtime (mins),Year,Genres,Num Votes,Release Date,Directors
                1,tt0111161,2024-01-01,2024-01-01,,The Shawshank Redemption,https://www.imdb.com/title/tt0111161/,Movie,9.3,142,1994,"Drama, Crime",2800000,1994-09-23,Frank Darabont
                2,tt9999999,2024-01-01,2024-01-01,,Upcoming Short,https://www.imdb.com/title/tt9999999/,Short,,,,Drama,,,""
                """;

        OpenCsvWatchlistParser parser = new OpenCsvWatchlistParser();
        List<MediaItem> items = parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).imdbId()).isEqualTo(new ImdbId("tt0111161"));
        assertThat(items.get(0).rating()).isEqualTo(Rating.of(9.3));
        assertThat(items.get(0).genres()).containsExactlyInAnyOrder(Genre.DRAMA, Genre.CRIME);
        assertThat(items.get(1).mediaType()).isEqualTo(MediaType.SHORT);
        assertThat(items.get(1).rating()).isNull();
        assertThat(items.get(1).releaseYear()).isNull();
    }
}

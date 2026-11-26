package com.imdbselector.infrastructure.adapter.out.csv;

import com.imdbselector.application.port.out.CsvWatchlistParserPort;
import com.imdbselector.domain.exception.InvalidCsvException;
import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.ImdbId;
import com.imdbselector.domain.valueobject.MediaType;
import com.imdbselector.domain.valueobject.MetadataSource;
import com.imdbselector.domain.valueobject.Rating;
import com.imdbselector.domain.valueobject.ReleaseYear;
import com.opencsv.CSVReaderHeaderAware;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Header-mapped IMDB CSV export parser resilient to column reordering and missing optional fields.
 */
public final class OpenCsvWatchlistParser implements CsvWatchlistParserPort {

    @Override
    public List<MediaItem> parse(InputStream csvStream) {
        if (csvStream == null) {
            throw new InvalidCsvException("CSV input stream must not be null");
        }
        List<MediaItem> items = new ArrayList<>();
        try (var reader = new CSVReaderHeaderAware(new InputStreamReader(csvStream, StandardCharsets.UTF_8))) {
            Map<String, String> row;
            while ((row = reader.readMap()) != null) {
                String rawId = firstPresent(row, "Const", "const");
                String rawTitle = firstPresent(row, "Title", "title");
                if (rawId == null || rawId.isBlank() || rawTitle == null || rawTitle.isBlank()) {
                    continue;
                }
                ImdbId imdbId = new ImdbId(rawId);
                String director = nullToEmpty(firstPresent(row, "Directors", "Director"));
                ReleaseYear year = parseYear(firstPresent(row, "Year"));
                Rating rating = parseRating(firstPresent(row, "IMDb Rating", "Rating"));
                MediaType mediaType = MediaType.fromLabel(firstPresent(row, "Title Type", "Type"));
                String url = firstPresent(row, "URL", "url");
                Set<Genre> genres = parseGenres(firstPresent(row, "Genres", "Genre"));

                items.add(new MediaItem(
                        imdbId,
                        rawTitle,
                        director,
                        year,
                        rating,
                        mediaType,
                        url,
                        genres,
                        MetadataSource.CSV
                ));
            }
        } catch (IOException | CsvValidationException | IllegalArgumentException e) {
            throw new InvalidCsvException("Failed to parse IMDB CSV: " + e.getMessage());
        }
        if (items.isEmpty()) {
            throw new InvalidCsvException("CSV contained no valid IMDB media items");
        }
        return items;
    }

    private static String firstPresent(Map<String, String> row, String... keys) {
        for (String key : keys) {
            for (var entry : row.entrySet()) {
                if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(key)) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private static ReleaseYear parseYear(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new ReleaseYear(Integer.parseInt(raw.trim()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Rating parseRating(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Rating.of(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Set<Genre> parseGenres(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Genre::fromLabel)
                .collect(Collectors.toSet());
    }
}

package com.imdbselector.domain.valueobject;

import java.util.Arrays;

/**
 * Standard IMDB title/media types.
 */
public enum MediaType {
    MOVIE("Movie"),
    TV_SERIES("TV Series"),
    SHORT("Short"),
    TV_EPISODE("TV Episode"),
    TV_MINI_SERIES("TV Mini Series"),
    TV_MOVIE("TV Movie"),
    TV_SPECIAL("TV Special"),
    TV_SHORT("TV Short"),
    VIDEO_GAME("Video Game"),
    VIDEO("Video"),
    MUSIC_VIDEO("Music Video"),
    PODCAST_SERIES("Podcast Series"),
    PODCAST_EPISODE("Podcast Episode"),
    OTHER("Other");

    private final String label;

    MediaType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static MediaType fromLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        String normalized = raw.trim();
        return Arrays.stream(values())
                .filter(t -> t.label.equalsIgnoreCase(normalized) || t.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(OTHER);
    }
}

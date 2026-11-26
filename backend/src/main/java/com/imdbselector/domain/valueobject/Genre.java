package com.imdbselector.domain.valueobject;

import java.util.Arrays;

/**
 * Standard IMDB genres.
 */
public enum Genre {
    ACTION("Action"),
    ADVENTURE("Adventure"),
    ANIMATION("Animation"),
    BIOGRAPHY("Biography"),
    COMEDY("Comedy"),
    CRIME("Crime"),
    DOCUMENTARY("Documentary"),
    DRAMA("Drama"),
    FAMILY("Family"),
    FANTASY("Fantasy"),
    FILM_NOIR("Film-Noir"),
    GAME_SHOW("Game-Show"),
    HISTORY("History"),
    HORROR("Horror"),
    MUSIC("Music"),
    MUSICAL("Musical"),
    MYSTERY("Mystery"),
    NEWS("News"),
    REALITY_TV("Reality-TV"),
    ROMANCE("Romance"),
    SCI_FI("Sci-Fi"),
    SHORT("Short"),
    SPORT("Sport"),
    TALK_SHOW("Talk-Show"),
    THRILLER("Thriller"),
    WAR("War"),
    WESTERN("Western"),
    OTHER("Other");

    private final String label;

    Genre(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static Genre fromLabel(String raw) {
        if (raw == null || raw.isBlank()) {
            return OTHER;
        }
        String normalized = raw.trim();
        return Arrays.stream(values())
                .filter(g -> g.label.equalsIgnoreCase(normalized) || g.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElse(OTHER);
    }
}

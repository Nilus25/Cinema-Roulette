package com.imdbselector.domain.query;

import com.imdbselector.domain.model.MediaItem;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.MediaType;

import java.util.Locale;
import java.util.Objects;

/**
 * Immutable compiled boolean filter AST evaluated directly against MediaItem instances.
 */
public sealed interface QueryExpression
        permits QueryExpression.And,
                QueryExpression.Or,
                QueryExpression.Not,
                QueryExpression.GenreMatch,
                QueryExpression.MediaTypeMatch,
                QueryExpression.NumericComparison,
                QueryExpression.TextMatch,
                QueryExpression.SeenMatch {

    boolean evaluate(MediaItem item, boolean seen);

    int cost();

    static QueryExpression and(QueryExpression left, QueryExpression right) {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");
        if (left.cost() <= right.cost()) {
            return new And(left, right);
        }
        return new And(right, left);
    }

    static QueryExpression or(QueryExpression left, QueryExpression right) {
        Objects.requireNonNull(left, "left must not be null");
        Objects.requireNonNull(right, "right must not be null");
        if (left.cost() <= right.cost()) {
            return new Or(left, right);
        }
        return new Or(right, left);
    }

    static QueryExpression not(QueryExpression operand) {
        Objects.requireNonNull(operand, "operand must not be null");
        if (operand instanceof Not nested) {
            return nested.operand();
        }
        if (operand instanceof SeenMatch seenMatch) {
            return new SeenMatch(!seenMatch.expectedSeen());
        }
        return new Not(operand);
    }

    record And(QueryExpression left, QueryExpression right) implements QueryExpression {
        public And {
            Objects.requireNonNull(left, "left must not be null");
            Objects.requireNonNull(right, "right must not be null");
        }

        @Override
        public boolean evaluate(MediaItem item, boolean seen) {
            return left.evaluate(item, seen) && right.evaluate(item, seen);
        }

        @Override
        public int cost() {
            return left.cost() + right.cost();
        }
    }

    record Or(QueryExpression left, QueryExpression right) implements QueryExpression {
        public Or {
            Objects.requireNonNull(left, "left must not be null");
            Objects.requireNonNull(right, "right must not be null");
        }

        @Override
        public boolean evaluate(MediaItem item, boolean seen) {
            return left.evaluate(item, seen) || right.evaluate(item, seen);
        }

        @Override
        public int cost() {
            return left.cost() + right.cost();
        }
    }

    record Not(QueryExpression operand) implements QueryExpression {
        public Not {
            Objects.requireNonNull(operand, "operand must not be null");
        }

        @Override
        public boolean evaluate(MediaItem item, boolean seen) {
            return !operand.evaluate(item, seen);
        }

        @Override
        public int cost() {
            return 1 + operand.cost();
        }
    }

    record GenreMatch(Genre genre) implements QueryExpression {
        public GenreMatch {
            Objects.requireNonNull(genre, "genre must not be null");
        }

        @Override
        public boolean evaluate(MediaItem item, boolean seen) {
            return item.genres().contains(genre);
        }

        @Override
        public int cost() {
            return 1;
        }
    }

    record MediaTypeMatch(MediaType mediaType) implements QueryExpression {
        public MediaTypeMatch {
            Objects.requireNonNull(mediaType, "mediaType must not be null");
        }

        @Override
        public boolean evaluate(MediaItem item, boolean seen) {
            return item.mediaType() == mediaType;
        }

        @Override
        public int cost() {
            return 1;
        }
    }

    record NumericComparison(NumericField field, ComparisonOperator operator, double target) implements QueryExpression {
        public NumericComparison {
            Objects.requireNonNull(field, "field must not be null");
            Objects.requireNonNull(operator, "operator must not be null");
        }

        @Override
        public boolean evaluate(MediaItem item, boolean seen) {
            return switch (field) {
                case RATING -> item.rating() != null && operator.test(item.rating().value().doubleValue(), target);
                case YEAR -> item.releaseYear() != null && operator.test(item.releaseYear().value(), target);
                case RUNTIME -> item.runtimeMinutes() != null && operator.test(item.runtimeMinutes(), target);
                case VOTES -> item.numVotes() != null && operator.test(item.numVotes(), target);
            };
        }

        @Override
        public int cost() {
            return 2;
        }
    }

    record TextMatch(TextField field, String lowerNeedle) implements QueryExpression {
        public TextMatch {
            Objects.requireNonNull(field, "field must not be null");
            if (lowerNeedle == null || lowerNeedle.isBlank()) {
                throw new IllegalArgumentException("Text search needle must not be blank");
            }
            lowerNeedle = lowerNeedle.trim().toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean evaluate(MediaItem item, boolean seen) {
            return switch (field) {
                case TITLE -> containsIgnoreCase(item.title(), lowerNeedle)
                        || containsIgnoreCase(item.originalTitle(), lowerNeedle);
                case DIRECTOR -> containsIgnoreCase(item.director(), lowerNeedle);
                case KEYWORD -> containsIgnoreCase(item.title(), lowerNeedle)
                        || containsIgnoreCase(item.originalTitle(), lowerNeedle)
                        || containsIgnoreCase(item.director(), lowerNeedle);
            };
        }

        @Override
        public int cost() {
            return 10;
        }

        private static boolean containsIgnoreCase(String haystack, String lowerNeedle) {
            return haystack != null && !haystack.isEmpty() && haystack.toLowerCase(Locale.ROOT).contains(lowerNeedle);
        }
    }

    record SeenMatch(boolean expectedSeen) implements QueryExpression {
        @Override
        public boolean evaluate(MediaItem item, boolean seen) {
            return seen == expectedSeen;
        }

        @Override
        public int cost() {
            return 1;
        }
    }
}

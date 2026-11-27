package com.imdbselector.domain.query;

import com.imdbselector.domain.exception.InvalidQueryException;
import com.imdbselector.domain.valueobject.Genre;
import com.imdbselector.domain.valueobject.MediaType;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Recursive-descent LL(1) parser compiling filter query strings into an optimized QueryExpression AST.
 */
public final class QueryParser {
    static final int MAX_DEPTH = 10;

    private static final Map<String, Genre> GENRES_BY_KEY = buildGenreIndex();
    private static final Map<String, MediaType> MEDIA_TYPES_BY_KEY = buildMediaTypeIndex();

    private final List<QueryLexer.Token> tokens;
    private int cursor;
    private int depth;

    private QueryParser(List<QueryLexer.Token> tokens) {
        this.tokens = tokens;
        this.cursor = 0;
        this.depth = 0;
    }

    public static QueryExpression parse(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return null;
        }
        List<QueryLexer.Token> tokens = QueryLexer.tokenize(rawQuery);
        QueryParser parser = new QueryParser(tokens);
        QueryExpression expr = parser.parseOr();
        if (parser.current().type() != QueryLexer.TokenType.EOF) {
            QueryLexer.Token extra = parser.current();
            throw new InvalidQueryException(
                    "Unexpected token '" + extra.text() + "' at position " + extra.position());
        }
        return expr;
    }

    private QueryExpression parseOr() {
        QueryExpression left = parseAnd();
        while (current().type() == QueryLexer.TokenType.OR) {
            advance();
            QueryExpression right = parseAnd();
            left = QueryExpression.or(left, right);
        }
        return left;
    }

    private QueryExpression parseAnd() {
        QueryExpression left = parseNot();
        while (current().type() == QueryLexer.TokenType.AND) {
            advance();
            QueryExpression right = parseNot();
            left = QueryExpression.and(left, right);
        }
        return left;
    }

    private QueryExpression parseNot() {
        if (current().type() == QueryLexer.TokenType.NOT) {
            enterDepth(current().position());
            advance();
            QueryExpression operand = parseNot();
            depth--;
            return QueryExpression.not(operand);
        }
        return parsePrimary();
    }

    private QueryExpression parsePrimary() {
        QueryLexer.Token token = current();
        if (token.type() == QueryLexer.TokenType.LPAREN) {
            enterDepth(token.position());
            advance();
            QueryExpression inner = parseOr();
            if (current().type() != QueryLexer.TokenType.RPAREN) {
                throw new InvalidQueryException(
                        "Expected closing ')' at position " + current().position());
            }
            advance();
            depth--;
            return inner;
        }

        if (token.type() == QueryLexer.TokenType.STRING) {
            advance();
            return resolveBareTerm(token.text(), token.position());
        }

        if (token.type() == QueryLexer.TokenType.WORD) {
            String lower = token.text().toLowerCase(Locale.ROOT);

            Optional<NumericField> numericField = NumericField.fromToken(lower);
            if (numericField.isPresent() && isComparisonOperator(peek().type())) {
                advance();
                ComparisonOperator op = parseComparisonOperator();
                double value = parseNumberValue();
                return new QueryExpression.NumericComparison(numericField.get(), op, value);
            }

            Optional<TextField> textField = TextField.fromToken(lower);
            if (textField.isPresent() && isTextAssignment(peek().type())) {
                advance();
                advance();
                String needle = parseValueToken();
                return new QueryExpression.TextMatch(textField.get(), needle);
            }

            if (lower.equals("genre") && isTextAssignment(peek().type())) {
                advance();
                advance();
                int valuePos = current().position();
                String rawGenre = parsePhraseOrQuoted();
                Genre genre = lookupGenre(rawGenre);
                if (genre == null) {
                    throw new InvalidQueryException("Unknown genre '" + rawGenre + "' at position " + valuePos);
                }
                return new QueryExpression.GenreMatch(genre);
            }

            if ((lower.equals("type") || lower.equals("mediatype") || lower.equals("media_type"))
                    && isTextAssignment(peek().type())) {
                advance();
                advance();
                int valuePos = current().position();
                String rawType = parsePhraseOrQuoted();
                MediaType mediaType = lookupMediaType(rawType);
                if (mediaType == null) {
                    throw new InvalidQueryException("Unknown media type '" + rawType + "' at position " + valuePos);
                }
                return new QueryExpression.MediaTypeMatch(mediaType);
            }

            if (lower.equals("seen")) {
                advance();
                if (current().type() == QueryLexer.TokenType.EQ || current().type() == QueryLexer.TokenType.NEQ) {
                    boolean negated = current().type() == QueryLexer.TokenType.NEQ;
                    advance();
                    boolean flag = parseBooleanValue();
                    return new QueryExpression.SeenMatch(negated != flag);
                }
                return new QueryExpression.SeenMatch(true);
            }

            if (lower.equals("unseen")) {
                advance();
                return new QueryExpression.SeenMatch(false);
            }

            int startPos = token.position();
            String phrase = parseUnquotedPhrase();
            return resolveBareTerm(phrase, startPos);
        }

        if (token.type() == QueryLexer.TokenType.EOF) {
            throw new InvalidQueryException("Unexpected end of query at position " + token.position());
        }
        throw new InvalidQueryException("Unexpected token '" + token.text() + "' at position " + token.position());
    }

    private ComparisonOperator parseComparisonOperator() {
        QueryLexer.Token token = current();
        advance();
        return switch (token.type()) {
            case LT -> ComparisonOperator.LT;
            case LTE -> ComparisonOperator.LTE;
            case GT -> ComparisonOperator.GT;
            case GTE -> ComparisonOperator.GTE;
            case EQ -> ComparisonOperator.EQ;
            case NEQ -> ComparisonOperator.NEQ;
            default -> throw new InvalidQueryException(
                    "Expected comparison operator (<, <=, >, >=, ==, !=) at position " + token.position());
        };
    }

    private double parseNumberValue() {
        QueryLexer.Token token = current();
        if (token.type() != QueryLexer.TokenType.NUMBER) {
            throw new InvalidQueryException("Expected number at position " + token.position());
        }
        advance();
        try {
            return Double.parseDouble(token.text());
        } catch (NumberFormatException e) {
            throw new InvalidQueryException("Invalid number '" + token.text() + "' at position " + token.position());
        }
    }

    private boolean parseBooleanValue() {
        QueryLexer.Token token = current();
        if (token.type() != QueryLexer.TokenType.WORD) {
            throw new InvalidQueryException("Expected 'true' or 'false' at position " + token.position());
        }
        advance();
        String lower = token.text().toLowerCase(Locale.ROOT);
        if (lower.equals("true")) {
            return true;
        }
        if (lower.equals("false")) {
            return false;
        }
        throw new InvalidQueryException("Expected 'true' or 'false' at position " + token.position());
    }

    private String parseValueToken() {
        QueryLexer.Token token = current();
        if (token.type() == QueryLexer.TokenType.STRING
                || token.type() == QueryLexer.TokenType.WORD
                || token.type() == QueryLexer.TokenType.NUMBER) {
            advance();
            if (token.text().isBlank()) {
                throw new InvalidQueryException("Search value must not be blank at position " + token.position());
            }
            return token.text();
        }
        throw new InvalidQueryException("Expected text value at position " + token.position());
    }

    private String parsePhraseOrQuoted() {
        if (current().type() == QueryLexer.TokenType.STRING) {
            QueryLexer.Token token = current();
            advance();
            return token.text();
        }
        return parseUnquotedPhrase();
    }

    private String parseUnquotedPhrase() {
        if (current().type() != QueryLexer.TokenType.WORD) {
            throw new InvalidQueryException(
                    "Expected term at position " + current().position());
        }
        StringBuilder sb = new StringBuilder(current().text());
        advance();
        while (current().type() == QueryLexer.TokenType.WORD) {
            sb.append(' ').append(current().text());
            advance();
        }
        return sb.toString();
    }

    private QueryExpression resolveBareTerm(String rawTerm, int position) {
        Genre genre = lookupGenre(rawTerm);
        MediaType mediaType = lookupMediaType(rawTerm);
        if (genre != null && mediaType != null) {
            return QueryExpression.or(new QueryExpression.GenreMatch(genre), new QueryExpression.MediaTypeMatch(mediaType));
        }
        if (genre != null) {
            return new QueryExpression.GenreMatch(genre);
        }
        if (mediaType != null) {
            return new QueryExpression.MediaTypeMatch(mediaType);
        }
        throw new InvalidQueryException(
                "Unknown genre or media type '" + rawTerm + "' at position " + position
                        + ". Use valid genres/types (e.g. Horror, Movie, \"TV Series\") or field filters (e.g. rating >= 8, title:\"Matrix\").");
    }

    private void enterDepth(int position) {
        depth++;
        if (depth > MAX_DEPTH) {
            throw new InvalidQueryException(
                    "Query nesting exceeds maximum depth of " + MAX_DEPTH + " at position " + position);
        }
    }

    private static boolean isComparisonOperator(QueryLexer.TokenType type) {
        return type == QueryLexer.TokenType.LT
                || type == QueryLexer.TokenType.LTE
                || type == QueryLexer.TokenType.GT
                || type == QueryLexer.TokenType.GTE
                || type == QueryLexer.TokenType.EQ
                || type == QueryLexer.TokenType.NEQ;
    }

    private static boolean isTextAssignment(QueryLexer.TokenType type) {
        return type == QueryLexer.TokenType.COLON || type == QueryLexer.TokenType.EQ;
    }

    private QueryLexer.Token current() {
        return tokens.get(cursor);
    }

    private QueryLexer.Token peek() {
        int nextIndex = Math.min(cursor + 1, tokens.size() - 1);
        return tokens.get(nextIndex);
    }

    private void advance() {
        if (cursor < tokens.size() - 1) {
            cursor++;
        }
    }

    private static Genre lookupGenre(String raw) {
        return GENRES_BY_KEY.get(normalizeKey(raw));
    }

    private static MediaType lookupMediaType(String raw) {
        return MEDIA_TYPES_BY_KEY.get(normalizeKey(raw));
    }

    private static String normalizeKey(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    private static Map<String, Genre> buildGenreIndex() {
        Map<String, Genre> map = new HashMap<>();
        for (Genre g : Genre.values()) {
            map.put(normalizeKey(g.label()), g);
            map.put(normalizeKey(g.name()), g);
        }
        return Map.copyOf(map);
    }

    private static Map<String, MediaType> buildMediaTypeIndex() {
        Map<String, MediaType> map = new HashMap<>();
        for (MediaType t : MediaType.values()) {
            map.put(normalizeKey(t.label()), t);
            map.put(normalizeKey(t.name()), t);
        }
        return Map.copyOf(map);
    }
}

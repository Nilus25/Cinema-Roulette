package com.imdbselector.domain.query;

import com.imdbselector.domain.exception.InvalidQueryException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class QueryLexer {
    static final int MAX_QUERY_LENGTH = 1000;
    static final int MAX_TOKENS = 100;

    enum TokenType {
        LPAREN,
        RPAREN,
        AND,
        OR,
        NOT,
        LT,
        LTE,
        GT,
        GTE,
        EQ,
        NEQ,
        COLON,
        NUMBER,
        STRING,
        WORD,
        EOF
    }

    record Token(TokenType type, String text, int position) {
    }

    private QueryLexer() {
    }

    static List<Token> tokenize(String input) {
        if (input == null) {
            throw new InvalidQueryException("Query must not be null");
        }
        if (input.length() > MAX_QUERY_LENGTH) {
            throw new InvalidQueryException("Query exceeds maximum length of " + MAX_QUERY_LENGTH + " characters");
        }

        List<Token> tokens = new ArrayList<>();
        int length = input.length();
        int i = 0;

        while (i < length) {
            char c = input.charAt(i);
            if (Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (tokens.size() >= MAX_TOKENS) {
                throw new InvalidQueryException("Query exceeds maximum token limit of " + MAX_TOKENS);
            }

            if (c == '(') {
                tokens.add(new Token(TokenType.LPAREN, "(", i));
                i++;
            } else if (c == ')') {
                tokens.add(new Token(TokenType.RPAREN, ")", i));
                i++;
            } else if (c == ':') {
                tokens.add(new Token(TokenType.COLON, ":", i));
                i++;
            } else if (c == '<') {
                if (i + 1 < length && input.charAt(i + 1) == '=') {
                    tokens.add(new Token(TokenType.LTE, "<=", i));
                    i += 2;
                } else if (i + 1 < length && input.charAt(i + 1) == '>') {
                    tokens.add(new Token(TokenType.NEQ, "<>", i));
                    i += 2;
                } else {
                    tokens.add(new Token(TokenType.LT, "<", i));
                    i++;
                }
            } else if (c == '>') {
                if (i + 1 < length && input.charAt(i + 1) == '=') {
                    tokens.add(new Token(TokenType.GTE, ">=", i));
                    i += 2;
                } else {
                    tokens.add(new Token(TokenType.GT, ">", i));
                    i++;
                }
            } else if (c == '=') {
                if (i + 1 < length && input.charAt(i + 1) == '=') {
                    tokens.add(new Token(TokenType.EQ, "==", i));
                    i += 2;
                } else {
                    tokens.add(new Token(TokenType.EQ, "=", i));
                    i++;
                }
            } else if (c == '!') {
                if (i + 1 < length && input.charAt(i + 1) == '=') {
                    tokens.add(new Token(TokenType.NEQ, "!=", i));
                    i += 2;
                } else {
                    tokens.add(new Token(TokenType.NOT, "!", i));
                    i++;
                }
            } else if (c == '&' && i + 1 < length && input.charAt(i + 1) == '&') {
                tokens.add(new Token(TokenType.AND, "&&", i));
                i += 2;
            } else if (c == '|' && i + 1 < length && input.charAt(i + 1) == '|') {
                tokens.add(new Token(TokenType.OR, "||", i));
                i += 2;
            } else if (c == '\u2227') {
                tokens.add(new Token(TokenType.AND, "\u2227", i));
                i++;
            } else if (c == '\u2228') {
                tokens.add(new Token(TokenType.OR, "\u2228", i));
                i++;
            } else if (c == '\u00AC') {
                tokens.add(new Token(TokenType.NOT, "\u00AC", i));
                i++;
            } else if (c == '"' || c == '\'') {
                int start = i;
                char quote = c;
                i++;
                StringBuilder sb = new StringBuilder();
                boolean closed = false;
                while (i < length) {
                    char ch = input.charAt(i);
                    if (ch == '\\' && i + 1 < length) {
                        sb.append(input.charAt(i + 1));
                        i += 2;
                    } else if (ch == quote) {
                        closed = true;
                        i++;
                        break;
                    } else {
                        sb.append(ch);
                        i++;
                    }
                }
                if (!closed) {
                    throw new InvalidQueryException("Unterminated quoted string starting at position " + start);
                }
                tokens.add(new Token(TokenType.STRING, sb.toString(), start));
            } else if (Character.isDigit(c)) {
                int start = i;
                boolean seenDot = false;
                while (i < length) {
                    char ch = input.charAt(i);
                    if (Character.isDigit(ch)) {
                        i++;
                    } else if (ch == '.' && !seenDot && i + 1 < length && Character.isDigit(input.charAt(i + 1))) {
                        seenDot = true;
                        i++;
                    } else {
                        break;
                    }
                }
                tokens.add(new Token(TokenType.NUMBER, input.substring(start, i), start));
            } else if (isWordStart(c)) {
                int start = i;
                while (i < length && isWordPart(input.charAt(i))) {
                    i++;
                }
                String word = input.substring(start, i);
                String upper = word.toUpperCase(Locale.ROOT);
                TokenType type = switch (upper) {
                    case "AND" -> TokenType.AND;
                    case "OR" -> TokenType.OR;
                    case "NOT" -> TokenType.NOT;
                    default -> TokenType.WORD;
                };
                tokens.add(new Token(type, word, start));
            } else {
                throw new InvalidQueryException("Unexpected character '" + c + "' at position " + i);
            }
        }

        tokens.add(new Token(TokenType.EOF, "", length));
        return tokens;
    }

    private static boolean isWordStart(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private static boolean isWordPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }
}

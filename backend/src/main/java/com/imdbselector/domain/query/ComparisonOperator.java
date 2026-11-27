package com.imdbselector.domain.query;

public enum ComparisonOperator {
    LT("<"),
    LTE("<="),
    GT(">"),
    GTE(">="),
    EQ("=="),
    NEQ("!=");

    private final String symbol;

    ComparisonOperator(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    public boolean test(double actual, double target) {
        int cmp = Double.compare(actual, target);
        return switch (this) {
            case LT -> cmp < 0;
            case LTE -> cmp <= 0;
            case GT -> cmp > 0;
            case GTE -> cmp >= 0;
            case EQ -> cmp == 0;
            case NEQ -> cmp != 0;
        };
    }
}

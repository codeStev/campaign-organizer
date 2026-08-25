package com.campaignorganizer.tables.domain.rolltable;

import com.campaignorganizer.shared.domain.ValidationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a dice expression and derives its closed result range {@code [min..max]}
 * (FR-40). Same term grammar as the FR-19 roller ({@code 2d6+3}, {@code d20},
 * {@code 2d20kh1}); keep-high/keep-low collapses to "keep K dice", whose range
 * is {@code K..K*sides} regardless of direction. Pure domain logic — used at
 * save time so entries map onto concrete totals without rolling.
 */
public final class DiceExpression {

    private static final int MAX_DICE = 100;
    private static final int MAX_SIDES = 1000;
    // A term is an optional sign, then either NdM(kh|kl K) or a plain integer.
    private static final Pattern TERM = Pattern.compile(
            "([+-]?)\\s*(?:(\\d*)d(\\d+)(?:k([hl])(\\d+))?|(\\d+))", Pattern.CASE_INSENSITIVE);

    private DiceExpression() {
    }

    /** Inclusive range of totals the expression can produce. */
    public record Range(int min, int max) {
    }

    public static Range range(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new ValidationException("Empty dice expression");
        }
        String expr = expression.replaceAll("\\s+", "");
        Matcher m = TERM.matcher(expr);
        int min = 0;
        int max = 0;
        int matchedChars = 0;
        boolean sawTerm = false;

        while (m.find()) {
            if (m.start() != matchedChars) {
                break; // gap => invalid character sequence
            }
            matchedChars = m.end();
            sawTerm = true;
            boolean negative = "-".equals(m.group(1));

            int termMin;
            int termMax;
            if (m.group(6) != null) {
                termMin = termMax = Integer.parseInt(m.group(6));
            } else {
                int count = m.group(2).isEmpty() ? 1 : Integer.parseInt(m.group(2));
                int sides = Integer.parseInt(m.group(3));
                if (count < 1 || count > MAX_DICE) {
                    throw new ValidationException("Dice count must be between 1 and " + MAX_DICE);
                }
                if (sides < 1 || sides > MAX_SIDES) {
                    throw new ValidationException("Die must have between 1 and " + MAX_SIDES + " sides");
                }
                if (m.group(4) != null) {
                    int kept = Integer.parseInt(m.group(5));
                    if (kept < 1 || kept > count) {
                        throw new ValidationException(
                                "Keep count must be between 1 and the number of dice");
                    }
                    termMin = kept;
                    termMax = kept * sides;
                } else {
                    termMin = count;
                    termMax = count * sides;
                }
            }
            if (negative) {
                min -= termMax;
                max -= termMin;
            } else {
                min += termMin;
                max += termMax;
            }
        }
        if (!sawTerm || matchedChars != expr.length()) {
            throw new ValidationException("Invalid dice expression: " + expression);
        }
        return new Range(min, max);
    }
}

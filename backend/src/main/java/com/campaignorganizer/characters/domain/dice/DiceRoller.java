package com.campaignorganizer.characters.domain.dice;

import com.campaignorganizer.shared.domain.ValidationException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and evaluates dice expressions such as {@code 2d6+3}, {@code d20-1}, or
 * {@code 2d20kh1} (keep highest = advantage). Pure domain logic — randomness comes
 * from a {@link RandomSource} so it is deterministic under test (FR-19).
 */
public final class DiceRoller {

    private static final int MAX_DICE = 100;
    private static final int MAX_SIDES = 1000;
    // A term is an optional sign, then either NdM(kh|kl K) or a plain integer.
    private static final Pattern TERM = Pattern.compile(
            "([+-]?)\\s*(?:(\\d*)d(\\d+)(?:k([hl])(\\d+))?|(\\d+))", Pattern.CASE_INSENSITIVE);

    private DiceRoller() {
    }

    public static RollResult roll(String expression, RandomSource rng) {
        if (expression == null || expression.isBlank()) {
            throw new ValidationException("Empty dice expression");
        }
        String expr = expression.replaceAll("\\s+", "");
        Matcher m = TERM.matcher(expr);
        List<DieRoll> rolls = new ArrayList<>();
        List<String> parts = new ArrayList<>();
        int total = 0;
        int matchedChars = 0;

        while (m.find()) {
            if (m.start() != matchedChars) {
                break; // gap => invalid character sequence
            }
            matchedChars = m.end();
            int sign = "-".equals(m.group(1)) ? -1 : 1;

            if (m.group(6) != null) {
                int constant = Integer.parseInt(m.group(6));
                total += sign * constant;
                parts.add((sign < 0 ? "- " : (parts.isEmpty() ? "" : "+ ")) + constant);
                continue;
            }

            int count = m.group(2).isEmpty() ? 1 : Integer.parseInt(m.group(2));
            int sides = Integer.parseInt(m.group(3));
            if (count < 1 || count > MAX_DICE) {
                throw new ValidationException("Dice count must be between 1 and " + MAX_DICE);
            }
            if (sides < 1 || sides > MAX_SIDES) {
                throw new ValidationException("Die must have between 1 and " + MAX_SIDES + " sides");
            }

            List<DieRoll> termRolls = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                termRolls.add(new DieRoll(sides, rng.nextInt(sides) + 1, true));
            }
            applyKeep(termRolls, m.group(4), m.group(5));

            int termTotal = termRolls.stream().filter(DieRoll::kept).mapToInt(DieRoll::value).sum();
            total += sign * termTotal;
            rolls.addAll(termRolls);
            String values = termRolls.stream()
                    .map(r -> r.kept() ? String.valueOf(r.value()) : "(" + r.value() + ")")
                    .reduce((a, b) -> a + ", " + b).orElse("");
            parts.add((sign < 0 ? "- " : (parts.isEmpty() ? "" : "+ "))
                    + count + "d" + sides + " [" + values + "]");
        }

        if (matchedChars != expr.length()) {
            throw new ValidationException("Invalid dice expression: " + expression);
        }
        return new RollResult(expression, total, rolls, String.join(" ", parts) + " = " + total);
    }

    /** Keep the highest/lowest {@code keepCount} dice, marking the rest as not kept. */
    private static void applyKeep(List<DieRoll> termRolls, String keepMode, String keepCountRaw) {
        if (keepMode == null) {
            return;
        }
        int keep = Math.min(Integer.parseInt(keepCountRaw), termRolls.size());
        Integer[] idx = new Integer[termRolls.size()];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = i;
        }
        Comparator<Integer> byValue = Comparator.comparingInt(i -> termRolls.get(i).value());
        Arrays.sort(idx, "h".equalsIgnoreCase(keepMode) ? byValue.reversed() : byValue);
        for (int rank = keep; rank < idx.length; rank++) {
            int i = idx[rank];
            DieRoll d = termRolls.get(i);
            termRolls.set(i, new DieRoll(d.sides(), d.value(), false));
        }
    }
}

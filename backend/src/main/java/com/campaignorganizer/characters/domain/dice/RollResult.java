package com.campaignorganizer.characters.domain.dice;

import java.util.List;

/** The outcome of evaluating a dice expression (value object). */
public record RollResult(String expression, int total, List<DieRoll> rolls, String breakdown) {
}

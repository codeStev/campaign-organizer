package com.campaignorganizer.characters.domain.dice;

/** One rolled die and whether it was kept (value object). */
public record DieRoll(int sides, int value, boolean kept) {
}

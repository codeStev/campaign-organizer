package com.campaignorganizer.characters.domain.dice;

/**
 * A source of randomness for dice rolls (domain-owned driven port). The application
 * supplies an implementation, so the roller is deterministic under test.
 */
public interface RandomSource {

    /** A value in {@code [0, bound)}. */
    int nextInt(int bound);
}

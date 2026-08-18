package com.campaignorganizer.characters.domain.dice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.util.Random;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/** Pure domain unit test with a seeded {@link RandomSource}. */
class DiceRollerTest {

    /** A deterministic source backed by a seeded {@link Random}. */
    private static RandomSource seeded(long seed) {
        return new Random(seed)::nextInt;
    }

    private static RandomSource anyRandom() {
        return new Random()::nextInt;
    }

    @Test
    void addsConstantModifier() {
        RollResult result = DiceRoller.roll("+5", seeded(1));
        assertThat(result.total()).isEqualTo(5);
    }

    @Test
    void rollsAreWithinBoundsAndSummed() {
        RollResult result = DiceRoller.roll("3d6+2", seeded(42));
        assertThat(result.rolls()).hasSize(3);
        assertThat(result.rolls()).allSatisfy(r -> assertThat(r.value()).isBetween(1, 6));
        int diceSum = result.rolls().stream().mapToInt(DieRoll::value).sum();
        assertThat(result.total()).isEqualTo(diceSum + 2);
    }

    @Test
    void subtractsTerms() {
        RollResult result = DiceRoller.roll("d20-1", seeded(7));
        int die = result.rolls().get(0).value();
        assertThat(result.total()).isEqualTo(die - 1);
    }

    @RepeatedTest(20)
    void keepHighestDropsTheLowerDie() {
        RollResult result = DiceRoller.roll("2d20kh1", anyRandom());
        assertThat(result.rolls()).hasSize(2);
        long kept = result.rolls().stream().filter(DieRoll::kept).count();
        assertThat(kept).isEqualTo(1);
        int keptValue = result.rolls().stream().filter(DieRoll::kept).findFirst().orElseThrow().value();
        int maxRolled = result.rolls().stream().mapToInt(DieRoll::value).max().orElseThrow();
        assertThat(keptValue).isEqualTo(maxRolled);
        assertThat(result.total()).isEqualTo(keptValue);
    }

    @RepeatedTest(20)
    void keepLowestForDisadvantage() {
        RollResult result = DiceRoller.roll("2d20kl1", anyRandom());
        int keptValue = result.rolls().stream().filter(DieRoll::kept).findFirst().orElseThrow().value();
        int minRolled = result.rolls().stream().mapToInt(DieRoll::value).min().orElseThrow();
        assertThat(keptValue).isEqualTo(minRolled);
    }

    @Test
    void combinesMultipleDiceTerms() {
        RollResult result = DiceRoller.roll("1d8+2d6+1", seeded(3));
        assertThat(result.rolls()).hasSize(3);
        assertThat(result.rolls().get(0).sides()).isEqualTo(8);
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> DiceRoller.roll("2d6+banana", anyRandom()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsTooManyDice() {
        assertThatThrownBy(() -> DiceRoller.roll("101d6", anyRandom()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsEmpty() {
        assertThatThrownBy(() -> DiceRoller.roll("   ", anyRandom()))
                .isInstanceOf(ValidationException.class);
    }
}

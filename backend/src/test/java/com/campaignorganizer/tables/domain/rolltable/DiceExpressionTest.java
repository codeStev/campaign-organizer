package com.campaignorganizer.tables.domain.rolltable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import org.junit.jupiter.api.Test;

class DiceExpressionTest {

    @Test
    void parsesSingleDie() {
        var range = DiceExpression.range("1d6");
        assertThat(range.min()).isEqualTo(1);
        assertThat(range.max()).isEqualTo(6);
    }

    @Test
    void countMayBeOmitted() {
        assertThat(DiceExpression.range("d20")).isEqualTo(new DiceExpression.Range(1, 20));
    }

    @Test
    void addsConstantModifier() {
        assertThat(DiceExpression.range("2d6+3")).isEqualTo(new DiceExpression.Range(5, 15));
    }

    @Test
    void combinesMultipleDiceTerms() {
        // 2 + 1 .. 12 + 4
        assertThat(DiceExpression.range("2d6+1d4")).isEqualTo(new DiceExpression.Range(3, 16));
    }

    @Test
    void keepHighCollapsesToKeptDiceRange() {
        // 4d6kh3 keeps 3 dice: 3..18 regardless of direction.
        assertThat(DiceExpression.range("4d6kh3")).isEqualTo(new DiceExpression.Range(3, 18));
        assertThat(DiceExpression.range("4d6kl3")).isEqualTo(new DiceExpression.Range(3, 18));
    }

    @Test
    void subtractedTermSwapsContribution() {
        // -(-1..-4) => net min 2-4=-2, max 12-1=11
        assertThat(DiceExpression.range("2d6-1d4")).isEqualTo(new DiceExpression.Range(-2, 11));
    }

    @Test
    void plainConstantOnly() {
        assertThat(DiceExpression.range("5")).isEqualTo(new DiceExpression.Range(5, 5));
        assertThat(DiceExpression.range("+5")).isEqualTo(new DiceExpression.Range(5, 5));
        assertThat(DiceExpression.range("-5")).isEqualTo(new DiceExpression.Range(-5, -5));
    }

    @Test
    void whitespaceIsTolerated() {
        assertThat(DiceExpression.range("2d6 + 1d4")).isEqualTo(new DiceExpression.Range(3, 16));
    }

    @Test
    void caseInsensitiveSuffixes() {
        assertThat(DiceExpression.range("2D8KH1")).isEqualTo(new DiceExpression.Range(1, 8));
    }

    @Test
    void rejectsJunk() {
        assertThatThrownBy(() -> DiceExpression.range(null)).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DiceExpression.range("   ")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DiceExpression.range("abc")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DiceExpression.range("1d")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DiceExpression.range("2d6x")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DiceExpression.range("0d6")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DiceExpression.range("101d6")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DiceExpression.range("1d1001")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DiceExpression.range("2d6kh0")).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> DiceExpression.range("2d6kh3")).isInstanceOf(ValidationException.class);
    }
}

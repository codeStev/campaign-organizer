package com.campaignorganizer.tables.domain.rolltable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RollTableTest {

    private static final Instant NOW = Instant.parse("2026-08-25T10:00:00Z");

    private static RollTableEntry entry(Integer min, Integer max, String body) {
        return new RollTableEntry(UUID.randomUUID(), min, max, body);
    }

    @Test
    void createDerivesRangeFromDiceExpression() {
        RollTable table = RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Weather",
                "Daily weather", "2d6", List.of(entry(2, 7, "Rain"), entry(8, 12, "Sun")), NOW);

        assertThat(table.getMinResult()).isEqualTo(2);
        assertThat(table.getMaxResult()).isEqualTo(12);
        assertThat(table.getEntries()).hasSize(2);
        assertThat(table.getCreatedAt()).isEqualTo(NOW);
        assertThat(table.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void entryWithoutBoundsCoversEverythingElse() {
        RollTable table = RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Weather", null,
                "1d20", List.of(entry(1, 19, "Ordinary"), entry(null, null, "Something strange")),
                NOW);
        assertThat(table.getEntries()).hasSize(2);
    }

    @Test
    void gapsBetweenEntriesAreAllowed() {
        RollTable table = RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Gaps", null,
                "1d10", List.of(entry(1, 4, "Low"), entry(9, 10, "High")), NOW);
        assertThat(table.getEntries()).hasSize(2);
    }

    @Test
    void rejectsHalfBoundedEntries() {
        assertThatThrownBy(() -> RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Half",
                null, "1d6", List.of(entry(1, null, "A")), NOW))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("both result bounds");
    }

    @Test
    void rejectsSecondCatchAllEntry() {
        assertThatThrownBy(() -> RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Twofold",
                null, "1d6", List.of(entry(null, null, "A"), entry(null, null, "B")), NOW))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("remaining results");
    }

    @Test
    void rejectsOverlappingEntries() {
        assertThatThrownBy(() -> RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Overlap",
                null, "1d10", List.of(entry(1, 5, "A"), entry(5, 7, "B")), NOW))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("overlap");
    }

    @Test
    void rejectsEntriesOutsideTheDiceRange() {
        assertThatThrownBy(() -> RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Out",
                null, "1d6", List.of(entry(0, 3, "A")), NOW))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("outside");
        assertThatThrownBy(() -> RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Out",
                null, "1d6", List.of(entry(1, 7, "A")), NOW))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("outside");
    }

    @Test
    void rejectsInvertedEntryRange() {
        assertThatThrownBy(() -> RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Inverted",
                null, "1d6", List.of(entry(4, 2, "A")), NOW))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("inverted");
    }

    @Test
    void rejectsBlankOrTooLongTitle() {
        assertThatThrownBy(() -> RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "  ",
                null, "1d6", List.of(), NOW))
                .isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "x".repeat(201),
                null, "1d6", List.of(), NOW))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateRecomputesRangeAndTimestamp() {
        RollTable table = RollTable.create(UUID.randomUUID(), UUID.randomUUID(), null, "Old", null,
                "1d6", List.of(entry(1, 3, "Heads-ish"), entry(4, 6, "Tails-ish")), NOW);

        Instant later = NOW.plusSeconds(60);
        table.update(null, "New", "desc", "1d10",
                List.of(entry(1, 5, "First half"), entry(6, 10, "Second half")), later);

        assertThat(table.getTitle()).isEqualTo("New");
        assertThat(table.getMinResult()).isEqualTo(1);
        assertThat(table.getMaxResult()).isEqualTo(10);
        assertThat(table.getUpdatedAt()).isEqualTo(later);
        assertThat(table.getCreatedAt()).isEqualTo(NOW);
    }
}

package com.campaignorganizer.handouts.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HandoutTest {

    private static final Instant NOW = Instant.EPOCH;

    @Test
    void nullBodyBecomesEmpty() {
        Handout h = Handout.create(UUID.randomUUID(), UUID.randomUUID(), "Letter",
                Handout.Preset.LETTER, null, null, NOW);
        assertThat(h.getBody()).isEmpty();
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> Handout.create(UUID.randomUUID(), UUID.randomUUID(), "  ",
                Handout.Preset.POSTER, "b", null, NOW))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsTitleOverTwoHundredChars() {
        assertThatThrownBy(() -> Handout.create(UUID.randomUUID(), UUID.randomUUID(),
                "x".repeat(201), Handout.Preset.POSTER, "b", null, NOW))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("200");
    }

    @Test
    void rejectsMissingPreset() {
        assertThatThrownBy(() -> Handout.create(UUID.randomUUID(), UUID.randomUUID(), "T",
                null, "b", null, NOW))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateChangesFieldsAndTouchTimestamp() {
        Handout h = Handout.create(UUID.randomUUID(), UUID.randomUUID(), "Old",
                Handout.Preset.PARCHMENT, "old body", null, NOW);

        UUID sessionId = UUID.randomUUID();
        h.update("New", Handout.Preset.NEWSPAPER, "new body", sessionId, NOW.plusSeconds(10));

        assertThat(h.getTitle()).isEqualTo("New");
        assertThat(h.getPreset()).isEqualTo(Handout.Preset.NEWSPAPER);
        assertThat(h.getBody()).isEqualTo("new body");
        assertThat(h.getSessionId()).isEqualTo(sessionId);
        assertThat(h.getUpdatedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void reorderSetsPositionAndTouchesTimestampOnly() {
        Handout h = Handout.create(UUID.randomUUID(), UUID.randomUUID(), "Old",
                Handout.Preset.PARCHMENT, "old body", null, NOW);

        h.reorder(3, NOW.plusSeconds(5));

        assertThat(h.getSortOrder()).isEqualTo(3);
        assertThat(h.getTitle()).isEqualTo("Old");
        assertThat(h.getUpdatedAt()).isEqualTo(NOW.plusSeconds(5));
    }
}

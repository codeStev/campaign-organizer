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
                Handout.Preset.LETTER, null, NOW);
        assertThat(h.getBody()).isEmpty();
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> Handout.create(UUID.randomUUID(), UUID.randomUUID(), "  ",
                Handout.Preset.POSTER, "b", NOW))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsTitleOverTwoHundredChars() {
        assertThatThrownBy(() -> Handout.create(UUID.randomUUID(), UUID.randomUUID(),
                "x".repeat(201), Handout.Preset.POSTER, "b", NOW))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("200");
    }

    @Test
    void rejectsMissingPreset() {
        assertThatThrownBy(() -> Handout.create(UUID.randomUUID(), UUID.randomUUID(), "T",
                null, "b", NOW))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateChangesFieldsAndTouchTimestamp() {
        Handout h = Handout.create(UUID.randomUUID(), UUID.randomUUID(), "Old",
                Handout.Preset.PARCHMENT, "old body", NOW);

        h.update("New", Handout.Preset.NEWSPAPER, "new body", NOW.plusSeconds(10));

        assertThat(h.getTitle()).isEqualTo("New");
        assertThat(h.getPreset()).isEqualTo(Handout.Preset.NEWSPAPER);
        assertThat(h.getBody()).isEqualTo("new body");
        assertThat(h.getUpdatedAt()).isEqualTo(NOW.plusSeconds(10));
    }
}

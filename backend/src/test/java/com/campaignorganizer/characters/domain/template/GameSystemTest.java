package com.campaignorganizer.characters.domain.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the game system aggregate (ADR-0094, ADR-0095). */
class GameSystemTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        GameSystem s = GameSystem.create(UUID.randomUUID(), "D&D 5e", "Crunchy d20 fantasy", "#c0392b",
                "SRD: https://example.com", T0);
        s.update("D&D 5e (revised)", "Still crunchy", "#e74c3c", "Updated notes", T1);

        assertThat(s.getName()).isEqualTo("D&D 5e (revised)");
        assertThat(s.getTagline()).isEqualTo("Still crunchy");
        assertThat(s.getColor()).isEqualTo("#e74c3c");
        assertThat(s.getNotes()).isEqualTo("Updated notes");
        assertThat(s.getCreatedAt()).isEqualTo(T0);
        assertThat(s.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void taglineColorAndNotesAreOptional() {
        GameSystem s = GameSystem.create(UUID.randomUUID(), "Triangle Agency", null, null, null, T0);

        assertThat(s.getTagline()).isNull();
        assertThat(s.getColor()).isNull();
        assertThat(s.getNotes()).isNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> GameSystem.create(UUID.randomUUID(), " ", null, null, null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> GameSystem.create(UUID.randomUUID(), null, null, null, null, T0))
                .isInstanceOf(ValidationException.class);
    }
}

package com.campaignorganizer.characters.domain.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the game system aggregate (ADR-0094). */
class GameSystemTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        GameSystem s = GameSystem.create(UUID.randomUUID(), "D&D 5e", T0);
        s.update("D&D 5e (revised)", T1);

        assertThat(s.getName()).isEqualTo("D&D 5e (revised)");
        assertThat(s.getCreatedAt()).isEqualTo(T0);
        assertThat(s.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> GameSystem.create(UUID.randomUUID(), " ", T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> GameSystem.create(UUID.randomUUID(), null, T0))
                .isInstanceOf(ValidationException.class);
    }
}

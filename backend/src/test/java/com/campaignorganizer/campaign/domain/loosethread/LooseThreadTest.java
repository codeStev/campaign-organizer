package com.campaignorganizer.campaign.domain.loosethread;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the loose-thread aggregate (ADR-0085). */
class LooseThreadTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void createDefaultsStatusToOpen() {
        LooseThread t = LooseThread.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "The stranger left a coin", null, T0);

        assertThat(t.getStatus()).isEqualTo(LooseThreadStatus.OPEN);
        assertThat(t.getCreatedAt()).isEqualTo(T0);
        assertThat(t.getUpdatedAt()).isEqualTo(T0);
    }

    @Test
    void updateBumpsUpdatedAtAndChangesStatus() {
        LooseThread t = LooseThread.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "Left a coin", LooseThreadStatus.OPEN, T0);

        t.update("Left a coin with a strange sigil", LooseThreadStatus.RESOLVED, T1);

        assertThat(t.getText()).isEqualTo("Left a coin with a strange sigil");
        assertThat(t.getStatus()).isEqualTo(LooseThreadStatus.RESOLVED);
        assertThat(t.getUpdatedAt()).isEqualTo(T1);
        assertThat(t.getCreatedAt()).isEqualTo(T0);
    }

    @Test
    void rejectsBlankText() {
        assertThatThrownBy(() -> LooseThread.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), " ", null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsOverlongText() {
        String tooLong = "x".repeat(2001);
        assertThatThrownBy(() -> LooseThread.create(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), tooLong, null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void canBeAbandonedWithoutBecomingResolved() {
        LooseThread t = LooseThread.create(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "A rumor nobody followed up on", LooseThreadStatus.OPEN, T0);

        t.update(t.getText(), LooseThreadStatus.ABANDONED, T1);

        assertThat(t.getStatus()).isEqualTo(LooseThreadStatus.ABANDONED);
    }
}

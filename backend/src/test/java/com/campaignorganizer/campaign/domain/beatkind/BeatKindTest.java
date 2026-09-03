package com.campaignorganizer.campaign.domain.beatkind;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the beat kind aggregate (ADR-0101). */
class BeatKindTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        UUID worldId = UUID.randomUUID();
        BeatKind k = BeatKind.create(UUID.randomUUID(), worldId, "Combat", "#c0392b", T0);
        k.update("Combat!", "#e74c3c", T1);

        assertThat(k.getName()).isEqualTo("Combat!");
        assertThat(k.getColor()).isEqualTo("#e74c3c");
        assertThat(k.getWorldId()).isEqualTo(worldId);
        assertThat(k.getCreatedAt()).isEqualTo(T0);
        assertThat(k.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void colorIsOptional() {
        BeatKind k = BeatKind.create(UUID.randomUUID(), UUID.randomUUID(), "Downtime", null, T0);

        assertThat(k.getColor()).isNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> BeatKind.create(UUID.randomUUID(), UUID.randomUUID(), " ", null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> BeatKind.create(UUID.randomUUID(), UUID.randomUUID(), null, null, T0))
                .isInstanceOf(ValidationException.class);
    }
}

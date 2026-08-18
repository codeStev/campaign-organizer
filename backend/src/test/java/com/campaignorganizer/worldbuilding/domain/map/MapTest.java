package com.campaignorganizer.worldbuilding.domain.map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the map aggregates. */
class MapTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void mapUpdateBumpsUpdatedAt() {
        UUID media = UUID.randomUUID();
        WorldMap m = WorldMap.create(UUID.randomUUID(), UUID.randomUUID(), "Overworld", media, T0);
        UUID newMedia = UUID.randomUUID();
        m.update("Underdark", newMedia, T1);

        assertThat(m.getName()).isEqualTo("Underdark");
        assertThat(m.getMediaId()).isEqualTo(newMedia);
        assertThat(m.getCreatedAt()).isEqualTo(T0);
        assertThat(m.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void mapRejectsBlankName() {
        assertThatThrownBy(() ->
                WorldMap.create(UUID.randomUUID(), UUID.randomUUID(), " ", UUID.randomUUID(), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void mapRequiresImage() {
        assertThatThrownBy(() ->
                WorldMap.create(UUID.randomUUID(), UUID.randomUUID(), "Overworld", null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void pinUpdateBumpsUpdatedAt() {
        MapPin p = MapPin.create(UUID.randomUUID(), UUID.randomUUID(), null, "start", "layer", 0.1, 0.2, T0);
        p.update(null, "end", "layer", 0.3, 0.4, T1);

        assertThat(p.getLabel()).isEqualTo("end");
        assertThat(p.getX()).isEqualTo(0.3);
        assertThat(p.getY()).isEqualTo(0.4);
        assertThat(p.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void pinRejectsCoordinatesOutOfRange() {
        assertThatThrownBy(() ->
                MapPin.create(UUID.randomUUID(), UUID.randomUUID(), null, null, null, 1.5, 0.2, T0))
                .isInstanceOf(ValidationException.class);
    }
}

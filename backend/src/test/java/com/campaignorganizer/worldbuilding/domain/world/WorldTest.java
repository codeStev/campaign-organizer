package com.campaignorganizer.worldbuilding.domain.world;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the world aggregate. */
class WorldTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        World w = World.create(UUID.randomUUID(), "Aetheria", "desc", T0);
        w.update("Aetheria II", "desc2", T1);

        assertThat(w.getName()).isEqualTo("Aetheria II");
        assertThat(w.getCreatedAt()).isEqualTo(T0);
        assertThat(w.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void createStartsWithEmptyLayerStyles() {
        World w = World.create(UUID.randomUUID(), "Aetheria", null, T0);
        assertThat(w.getLayerStyles()).isEmpty();
    }

    @Test
    void replaceLayerStylesBumpsUpdatedAt() {
        World w = World.create(UUID.randomUUID(), "Aetheria", null, T0);
        w.replaceLayerStyles(Map.of("towns", new LayerStyle("#ff0000", "castle")), T1);

        assertThat(w.getLayerStyles()).containsKey("towns");
        assertThat(w.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> World.create(UUID.randomUUID(), " ", null, T0))
                .isInstanceOf(ValidationException.class);
    }
}

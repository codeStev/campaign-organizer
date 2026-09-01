package com.campaignorganizer.campaign.domain.encounter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the encounter aggregate (ADR-0097). */
class EncounterTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");
    private static final UUID CAMPAIGN_ID = UUID.randomUUID();

    @Test
    void updateBumpsUpdatedAt() {
        UUID statblockId = UUID.randomUUID();
        Encounter e = Encounter.create(UUID.randomUUID(), CAMPAIGN_ID, "Goblin ambush", "watch the road",
                List.of(new EncounterEntry(statblockId, 3)), T0);
        e.update("Goblin ambush (revised)", "watch the bridge",
                List.of(new EncounterEntry(statblockId, 4)), T1);

        assertThat(e.getName()).isEqualTo("Goblin ambush (revised)");
        assertThat(e.getNotes()).isEqualTo("watch the bridge");
        assertThat(e.getEntries()).containsExactly(new EncounterEntry(statblockId, 4));
        assertThat(e.getCreatedAt()).isEqualTo(T0);
        assertThat(e.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void nullEntriesBecomeEmptyList() {
        Encounter e = Encounter.create(UUID.randomUUID(), CAMPAIGN_ID, "Empty room", null, null, T0);
        assertThat(e.getEntries()).isEmpty();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Encounter.create(UUID.randomUUID(), CAMPAIGN_ID, " ", null, List.of(), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsEntryWithNoStatblock() {
        assertThatThrownBy(() -> Encounter.create(UUID.randomUUID(), CAMPAIGN_ID, "Ambush", null,
                List.of(new EncounterEntry(null, 1)), T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsEntryWithZeroOrNegativeQuantity() {
        UUID statblockId = UUID.randomUUID();
        assertThatThrownBy(() -> Encounter.create(UUID.randomUUID(), CAMPAIGN_ID, "Ambush", null,
                List.of(new EncounterEntry(statblockId, 0)), T0))
                .isInstanceOf(ValidationException.class);
    }
}

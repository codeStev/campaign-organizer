package com.campaignorganizer.characters.domain.statblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the global (catalog) statblock aggregate (ADR-0096). */
class GlobalStatblockTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");
    private static final UUID SYSTEM_ID = UUID.randomUUID();

    @Test
    void updateBumpsUpdatedAt() {
        GlobalStatblock s = GlobalStatblock.create(UUID.randomUUID(), SYSTEM_ID, null, "Goblin Grunt",
                Map.of("HP", 7), "Nasty little thing", T0);
        UUID templateId = UUID.randomUUID();
        s.update(SYSTEM_ID, templateId, "Goblin Grunt (revised)", Map.of("HP", 9), "Nastier", T1);

        assertThat(s.getName()).isEqualTo("Goblin Grunt (revised)");
        assertThat(s.getGlobalTemplateId()).isEqualTo(templateId);
        assertThat(s.getStats()).isEqualTo(Map.of("HP", 9));
        assertThat(s.getNotes()).isEqualTo("Nastier");
        assertThat(s.getCreatedAt()).isEqualTo(T0);
        assertThat(s.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void nullStatsBecomeEmptyMap() {
        GlobalStatblock s = GlobalStatblock.create(UUID.randomUUID(), SYSTEM_ID, null, "Goblin Grunt", null,
                null, T0);
        assertThat(s.getStats()).isEmpty();
    }

    @Test
    void globalTemplateIdIsOptional() {
        GlobalStatblock s = GlobalStatblock.create(UUID.randomUUID(), SYSTEM_ID, null, "Goblin Grunt",
                Map.of(), null, T0);
        assertThat(s.getGlobalTemplateId()).isNull();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> GlobalStatblock.create(UUID.randomUUID(), SYSTEM_ID, null, " ", Map.of(),
                null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullSystem() {
        assertThatThrownBy(() -> GlobalStatblock.create(UUID.randomUUID(), null, null, "Goblin Grunt",
                Map.of(), null, T0))
                .isInstanceOf(ValidationException.class);
    }
}

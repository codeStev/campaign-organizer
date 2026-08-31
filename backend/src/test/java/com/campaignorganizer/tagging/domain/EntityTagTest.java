package com.campaignorganizer.tagging.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EntityTagTest {

    private static final Instant NOW = Instant.EPOCH;

    @Test
    void createFoldsNameToTrimmedLowercase() {
        EntityTag tag = EntityTag.create(UUID.randomUUID(), UUID.randomUUID(), EntityType.ARTICLE,
                UUID.randomUUID(), "  Recurring Villain  ", NOW);

        assertThat(tag.getName()).isEqualTo("recurring villain");
    }

    @Test
    void normalizeMatchesCreate() {
        assertThat(EntityTag.normalize("  NPC  ")).isEqualTo("npc");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> EntityTag.create(UUID.randomUUID(), UUID.randomUUID(),
                EntityType.STATBLOCK, UUID.randomUUID(), "   ", NOW))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNameOverMaxLength() {
        assertThatThrownBy(() -> EntityTag.create(UUID.randomUUID(), UUID.randomUUID(),
                EntityType.ARTICLE, UUID.randomUUID(), "x".repeat(101), NOW))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("100");
    }
}

package com.campaignorganizer.worldbuilding.domain.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the category aggregate. */
class CategoryTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        Category c = Category.create(UUID.randomUUID(), UUID.randomUUID(), null, "People", T0);
        UUID parent = UUID.randomUUID();
        c.update(parent, "Factions", T1);

        assertThat(c.getName()).isEqualTo("Factions");
        assertThat(c.getParentId()).isEqualTo(parent);
        assertThat(c.getCreatedAt()).isEqualTo(T0);
        assertThat(c.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() ->
                Category.create(UUID.randomUUID(), UUID.randomUUID(), null, " ", T0))
                .isInstanceOf(ValidationException.class);
    }
}

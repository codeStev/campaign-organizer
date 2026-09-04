package com.campaignorganizer.characters.domain.statblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the statblock aggregate. */
class StatblockTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        Statblock s = Statblock.create(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null,
                "Goblin", Map.of("hp", 7), "sneaky", T0);
        UUID article = UUID.randomUUID();
        s.update(null, article, null, null, null, "Hobgoblin", Map.of("hp", 11), "tougher", T1);

        assertThat(s.getName()).isEqualTo("Hobgoblin");
        assertThat(s.getArticleId()).isEqualTo(article);
        assertThat(s.getStats()).containsEntry("hp", 11);
        assertThat(s.getCreatedAt()).isEqualTo(T0);
        assertThat(s.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void nullStatsBecomeEmptyMap() {
        Statblock s = Statblock.create(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null,
                "Goblin", null, null, T0);
        assertThat(s.getStats()).isEmpty();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> Statblock.create(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null,
                null, " ", null, null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsBothWorldAndGlobalTemplateSet() {
        assertThatThrownBy(() -> Statblock.create(UUID.randomUUID(), UUID.randomUUID(), null, null, null,
                UUID.randomUUID(), UUID.randomUUID(), "Goblin", null, null, T0))
                .isInstanceOf(ValidationException.class);
    }
}

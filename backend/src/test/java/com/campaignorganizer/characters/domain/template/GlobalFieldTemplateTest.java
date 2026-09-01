package com.campaignorganizer.characters.domain.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the global field template aggregate (ADR-0093). */
class GlobalFieldTemplateTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        GlobalFieldTemplate t = GlobalFieldTemplate.create(UUID.randomUUID(), "D&D 5e",
                TemplateKind.CHARACTER, "dnd5e", List.of(), T0);
        t.update("D&D 5e v2", "dnd5e", List.of(), T1);

        assertThat(t.getName()).isEqualTo("D&D 5e v2");
        assertThat(t.getCreatedAt()).isEqualTo(T0);
        assertThat(t.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void nullSectionsBecomeEmptyList() {
        GlobalFieldTemplate t = GlobalFieldTemplate.create(UUID.randomUUID(), "D&D 5e",
                TemplateKind.CHARACTER, "dnd5e", null, T0);
        assertThat(t.getSections()).isEmpty();
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> GlobalFieldTemplate.create(UUID.randomUUID(), " ", TemplateKind.CHARACTER,
                "dnd5e", null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullKind() {
        assertThatThrownBy(() -> GlobalFieldTemplate.create(UUID.randomUUID(), "D&D 5e", null, "dnd5e",
                null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsBlankSystem() {
        assertThatThrownBy(() -> GlobalFieldTemplate.create(UUID.randomUUID(), "D&D 5e",
                TemplateKind.CHARACTER, " ", null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNullSystem() {
        assertThatThrownBy(() -> GlobalFieldTemplate.create(UUID.randomUUID(), "D&D 5e",
                TemplateKind.CHARACTER, null, null, T0))
                .isInstanceOf(ValidationException.class);
    }
}

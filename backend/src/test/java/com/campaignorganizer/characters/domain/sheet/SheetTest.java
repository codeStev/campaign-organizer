package com.campaignorganizer.characters.domain.sheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldTemplate;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the field template + character sheet aggregates. */
class SheetTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void templateUpdateBumpsUpdatedAt() {
        UUID systemId = UUID.randomUUID();
        FieldTemplate t = FieldTemplate.create(UUID.randomUUID(), UUID.randomUUID(), null, "Generic",
                TemplateKind.CHARACTER, systemId, List.of(), T0);
        t.update(null, "Generic v2", systemId, List.of(), T1);

        assertThat(t.getName()).isEqualTo("Generic v2");
        assertThat(t.getCreatedAt()).isEqualTo(T0);
        assertThat(t.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void templateNullSectionsBecomeEmptyList() {
        FieldTemplate t = FieldTemplate.create(UUID.randomUUID(), UUID.randomUUID(), null, "Generic",
                TemplateKind.CHARACTER, null, null, T0);
        assertThat(t.getSections()).isEmpty();
    }

    @Test
    void templateRejectsBlankName() {
        assertThatThrownBy(() ->
                FieldTemplate.create(UUID.randomUUID(), UUID.randomUUID(), null, " ", TemplateKind.CHARACTER, null,
                        null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void templateRejectsNullKind() {
        assertThatThrownBy(() ->
                FieldTemplate.create(UUID.randomUUID(), UUID.randomUUID(), null, "Generic", null, null, null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void characterSheetUpdateBumpsUpdatedAt() {
        UUID templateId = UUID.randomUUID();
        CharacterSheet s = CharacterSheet.create(UUID.randomUUID(), UUID.randomUUID(), null, templateId, null,
                null, null, "Aria", Map.of("hp", 10), T0);
        s.update(null, templateId, null, null, null, "Aria the Bold", Map.of("hp", 12), T1);

        assertThat(s.getName()).isEqualTo("Aria the Bold");
        assertThat(s.getValues()).containsEntry("hp", 12);
        assertThat(s.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void characterSheetRejectsBlankName() {
        assertThatThrownBy(() -> CharacterSheet.create(UUID.randomUUID(), UUID.randomUUID(), null,
                UUID.randomUUID(), null, null, null, " ", null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void characterSheetRequiresTemplate() {
        assertThatThrownBy(() -> CharacterSheet.create(UUID.randomUUID(), UUID.randomUUID(), null, null, null,
                null, null, "Aria", null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void characterSheetRejectsBothWorldAndGlobalTemplateSet() {
        assertThatThrownBy(() -> CharacterSheet.create(UUID.randomUUID(), UUID.randomUUID(), null,
                UUID.randomUUID(), UUID.randomUUID(), null, null, "Aria", null, T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void characterSheetAcceptsGlobalTemplateOnly() {
        UUID globalTemplateId = UUID.randomUUID();
        CharacterSheet s = CharacterSheet.create(UUID.randomUUID(), UUID.randomUUID(), null, null,
                globalTemplateId, null, null, "Aria", null, T0);
        assertThat(s.getGlobalTemplateId()).isEqualTo(globalTemplateId);
        assertThat(s.getWorldTemplateId()).isNull();
    }
}

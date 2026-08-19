package com.campaignorganizer.characters.application.sheet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.characters.application.sheet.port.in.CharacterSheetCommands.CreateCharacterSheetCommand;
import com.campaignorganizer.characters.application.sheet.port.out.ArticleExistsPort;
import com.campaignorganizer.characters.application.sheet.port.out.CampaignExistsPort;
import com.campaignorganizer.characters.application.sheet.port.out.CharacterSheetRepositoryPort;
import com.campaignorganizer.characters.application.sheet.port.out.WorldExistsPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.application.sheet.port.published.SheetTemplateQueryPort;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test for character sheets with mocked ports. */
@ExtendWith(MockitoExtension.class)
class CharacterSheetServiceTest {

    @Mock
    private CharacterSheetRepositoryPort sheets;
    @Mock
    private SheetTemplateQueryPort templates;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private ArticleExistsPort articles;
    @Mock
    private CampaignExistsPort campaigns;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final CharacterSheetViewMapper viewMapper = new CharacterSheetViewMapperImpl();

    private CharacterSheetService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID templateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CharacterSheetService(sheets, templates, worlds, articles, campaigns, viewMapper,
                ids, clock);
    }

    @Test
    void createSucceedsWithValidTemplate() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(templates.existsInWorld(templateId, worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(sheets.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CharacterSheetView view = service.create(new CreateCharacterSheetCommand(
                worldId, templateId, null, null, "Aria", null));

        assertThat(view.name()).isEqualTo("Aria");
    }

    @Test
    void createRejectsMissingWorld() {
        when(worlds.exists(worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateCharacterSheetCommand(
                worldId, templateId, null, null, "Aria", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignTemplate() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(templates.existsInWorld(templateId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateCharacterSheetCommand(
                worldId, templateId, null, null, "Aria", null)))
                .isInstanceOf(ValidationException.class);
    }
}

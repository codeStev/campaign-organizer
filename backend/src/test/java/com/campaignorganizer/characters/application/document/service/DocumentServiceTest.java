package com.campaignorganizer.characters.application.document.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.characters.application.document.port.in.DocumentCommands.CreateDocumentCommand;
import com.campaignorganizer.characters.application.document.port.out.CampaignExistsPort;
import com.campaignorganizer.characters.application.document.port.out.DocumentRepositoryPort;
import com.campaignorganizer.characters.application.document.port.out.WorldExistsPort;
import com.campaignorganizer.characters.application.document.port.published.DocumentView;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test for documents with mocked ports. */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepositoryPort documents;
    @Mock
    private FieldTemplateQueryPort templates;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private CampaignExistsPort campaigns;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final DocumentViewMapper viewMapper = new DocumentViewMapperImpl();

    private DocumentService service;

    private final UUID worldId = UUID.randomUUID();
    private final UUID templateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new DocumentService(documents, templates, worlds, campaigns, viewMapper, ids, clock);
    }

    private FieldTemplateView templateView(TemplateKind kind) {
        return new FieldTemplateView(templateId, worldId, "Template", kind, null, List.of(),
                Instant.now(), Instant.now());
    }

    @Test
    void createSucceedsWithValidTemplate() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(templates.findByIdInWorld(templateId, worldId))
                .thenReturn(Optional.of(templateView(TemplateKind.DOCUMENT)));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(documents.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DocumentView view = service.create(new CreateDocumentCommand(
                worldId, templateId, null, "Session Zero", null));

        assertThat(view.name()).isEqualTo("Session Zero");
    }

    @Test
    void createRejectsMissingWorld() {
        when(worlds.exists(worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateDocumentCommand(
                worldId, templateId, null, "Session Zero", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignTemplate() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(templates.findByIdInWorld(templateId, worldId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(new CreateDocumentCommand(
                worldId, templateId, null, "Session Zero", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsCharacterTemplate() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(templates.findByIdInWorld(templateId, worldId))
                .thenReturn(Optional.of(templateView(TemplateKind.CHARACTER)));

        assertThatThrownBy(() -> service.create(new CreateDocumentCommand(
                worldId, templateId, null, "Session Zero", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsStatblockTemplate() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(templates.findByIdInWorld(templateId, worldId))
                .thenReturn(Optional.of(templateView(TemplateKind.STATBLOCK)));

        assertThatThrownBy(() -> service.create(new CreateDocumentCommand(
                worldId, templateId, null, "Session Zero", null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsForeignCampaign() {
        UUID campaignId = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(templates.findByIdInWorld(templateId, worldId))
                .thenReturn(Optional.of(templateView(TemplateKind.DOCUMENT)));
        when(campaigns.existsInWorld(campaignId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateDocumentCommand(
                worldId, templateId, campaignId, "Session Zero", null)))
                .isInstanceOf(ValidationException.class);
    }
}

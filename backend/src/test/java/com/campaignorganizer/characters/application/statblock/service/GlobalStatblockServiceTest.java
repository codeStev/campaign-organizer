package com.campaignorganizer.characters.application.statblock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.characters.application.statblock.port.in.CreateStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.GlobalStatblockCommands.CreateGlobalStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.in.GlobalStatblockCommands.UpdateGlobalStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.in.StatblockCommands.CreateStatblockCommand;
import com.campaignorganizer.characters.application.statblock.port.out.GlobalStatblockRepositoryPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.characters.application.template.port.published.GameSystemQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
import com.campaignorganizer.characters.domain.statblock.GlobalStatblock;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test for the global statblock catalog (ADR-0096). */
@ExtendWith(MockitoExtension.class)
class GlobalStatblockServiceTest {

    @Mock
    private GlobalStatblockRepositoryPort statblocks;
    @Mock
    private GameSystemQueryPort systems;
    @Mock
    private GlobalFieldTemplateQueryPort templates;
    @Mock
    private CreateStatblockUseCase createStatblock;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final GlobalStatblockViewMapper viewMapper = new GlobalStatblockViewMapperImpl();

    private GlobalStatblockService service;

    private final UUID systemId = UUID.randomUUID();
    private final UUID otherSystemId = UUID.randomUUID();
    private final UUID templateId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GlobalStatblockService(statblocks, systems, templates, createStatblock, viewMapper,
                ids, clock);
    }

    private GlobalFieldTemplateView templateView(TemplateKind kind, UUID system) {
        return new GlobalFieldTemplateView(templateId, "Monster", kind, system, List.of(), Instant.now(),
                Instant.now());
    }

    @Test
    void createRejectsUnknownSystem() {
        when(systems.existsById(systemId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateGlobalStatblockCommand(systemId, null,
                "Goblin", Map.of(), null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsCrossSystemTemplate() {
        when(systems.existsById(systemId)).thenReturn(true);
        when(templates.findById(templateId)).thenReturn(Optional.of(templateView(TemplateKind.STATBLOCK,
                otherSystemId)));

        assertThatThrownBy(() -> service.create(new CreateGlobalStatblockCommand(systemId, templateId,
                "Goblin", Map.of(), null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsNonStatblockTemplate() {
        when(systems.existsById(systemId)).thenReturn(true);
        when(templates.findById(templateId)).thenReturn(Optional.of(templateView(TemplateKind.CHARACTER,
                systemId)));

        assertThatThrownBy(() -> service.create(new CreateGlobalStatblockCommand(systemId, templateId,
                "Goblin", Map.of(), null)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createSucceedsWithMatchingSystemTemplate() {
        when(systems.existsById(systemId)).thenReturn(true);
        when(templates.findById(templateId)).thenReturn(Optional.of(templateView(TemplateKind.STATBLOCK,
                systemId)));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(statblocks.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GlobalStatblockView view = service.create(new CreateGlobalStatblockCommand(systemId, templateId,
                "Goblin Grunt", Map.of("HP", 7), "Notes"));

        assertThat(view.name()).isEqualTo("Goblin Grunt");
        assertThat(view.systemId()).isEqualTo(systemId);
        assertThat(view.globalTemplateId()).isEqualTo(templateId);
    }

    @Test
    void deleteRemovesTheCatalogEntry() {
        UUID id = UUID.randomUUID();
        GlobalStatblock existing = GlobalStatblock.create(id, systemId, null, "Goblin", Map.of(), null,
                clock.instant());
        when(statblocks.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(statblocks).delete(existing);
    }

    @Test
    void updateRejectsUnknownEntry() {
        UUID id = UUID.randomUUID();
        when(statblocks.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(new UpdateGlobalStatblockCommand(id, systemId, null,
                "Goblin", Map.of(), null)))
                .isInstanceOf(NotFoundException.class);
    }

    // --- import into campaign (ADR-0096) ---

    @Test
    void importIntoCampaignRequiresCampaignId() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> service.importIntoCampaign(id, UUID.randomUUID(), null, null))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void importIntoCampaignCopiesStatsAndNotesWithoutABackReference() {
        UUID id = UUID.randomUUID();
        UUID worldId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        GlobalStatblock source = GlobalStatblock.create(id, systemId, templateId, "Adult Red Dragon",
                Map.of("HP", 256), "Breathes fire", clock.instant());
        when(statblocks.findById(id)).thenReturn(Optional.of(source));
        when(createStatblock.create(any())).thenReturn(new StatblockView(UUID.randomUUID(), worldId, null,
                campaignId, null, templateId, "Adult Red Dragon", Map.of("HP", 256), "Breathes fire",
                clock.instant(), clock.instant()));

        service.importIntoCampaign(id, worldId, campaignId, null);

        ArgumentCaptor<CreateStatblockCommand> captor = ArgumentCaptor.forClass(CreateStatblockCommand.class);
        verify(createStatblock).create(captor.capture());
        CreateStatblockCommand command = captor.getValue();
        assertThat(command.worldId()).isEqualTo(worldId);
        assertThat(command.campaignId()).isEqualTo(campaignId);
        assertThat(command.globalTemplateId()).isEqualTo(templateId);
        assertThat(command.name()).isEqualTo("Adult Red Dragon");
        assertThat(command.stats()).isEqualTo(Map.of("HP", 256));
        assertThat(command.notes()).isEqualTo("Breathes fire");
    }

    @Test
    void importIntoCampaignUsesNameOverrideWhenGiven() {
        UUID id = UUID.randomUUID();
        UUID worldId = UUID.randomUUID();
        UUID campaignId = UUID.randomUUID();
        GlobalStatblock source = GlobalStatblock.create(id, systemId, null, "Adult Red Dragon", Map.of(),
                null, clock.instant());
        when(statblocks.findById(id)).thenReturn(Optional.of(source));
        when(createStatblock.create(any())).thenReturn(new StatblockView(UUID.randomUUID(), worldId, null,
                campaignId, null, null, "Bahamut", Map.of(), null, clock.instant(), clock.instant()));

        service.importIntoCampaign(id, worldId, campaignId, "Bahamut");

        ArgumentCaptor<CreateStatblockCommand> captor = ArgumentCaptor.forClass(CreateStatblockCommand.class);
        verify(createStatblock).create(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Bahamut");
    }

    // --- published import port: resolve-or-reuse (ADR-0061/ADR-0096) ---

    @Test
    void importOrReuseReusesAnExistingMatchByNameAndSystem() {
        UUID existingId = UUID.randomUUID();
        GlobalStatblock existing = GlobalStatblock.create(existingId, systemId, null, "Goblin", Map.of(),
                null, clock.instant());
        when(statblocks.findBySystemIdAndName(systemId, "Goblin")).thenReturn(Optional.of(existing));

        GlobalStatblockView imported = new GlobalStatblockView(UUID.randomUUID(), systemId, null, "Goblin",
                Map.of(), null, clock.instant(), clock.instant());
        GlobalStatblockView result = service.importOrReuse(imported);

        assertThat(result.id()).isEqualTo(existingId);
    }

    @Test
    void importOrReuseCreatesWhenNoMatchExists() {
        when(statblocks.findBySystemIdAndName(systemId, "Goblin")).thenReturn(Optional.empty());
        when(statblocks.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UUID newId = UUID.randomUUID();
        GlobalStatblockView imported = new GlobalStatblockView(newId, systemId, null, "Goblin", Map.of(),
                null, clock.instant(), clock.instant());
        GlobalStatblockView result = service.importOrReuse(imported);

        assertThat(result.id()).isEqualTo(newId);
    }

    // --- published ref port (used by GlobalFieldTemplateService.delete()) ---

    @Test
    void existsReferencingGlobalTemplateDelegatesToRepository() {
        when(statblocks.existsByGlobalTemplateId(templateId)).thenReturn(true);

        assertThat(service.existsReferencingGlobalTemplate(templateId)).isTrue();
    }
}

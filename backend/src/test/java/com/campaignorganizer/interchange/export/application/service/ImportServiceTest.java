package com.campaignorganizer.interchange.export.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatImportPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.arc.port.published.ArcImportPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignImportPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerImportPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.domain.campaign.CampaignStatus;
import com.campaignorganizer.campaign.application.clock.port.published.ClockImportPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockSegmentView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadImportPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import com.campaignorganizer.campaign.application.player.port.published.PlayerImportPort;
import com.campaignorganizer.campaign.application.player.port.published.PlayerView;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetImportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionAttendanceImportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionAttendanceView;
import com.campaignorganizer.campaign.application.session.port.published.SessionImportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.characters.application.document.port.published.DocumentImportPort;
import com.campaignorganizer.characters.application.document.port.published.DocumentView;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetImportPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockImportPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateImportPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.media.application.port.published.MediaImportPort;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.handouts.application.port.published.HandoutImportPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckImportPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import com.campaignorganizer.tables.application.carddeck.port.published.DeckCardView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableEntryView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableImportPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import com.campaignorganizer.tagging.application.port.published.TagImportPort;
import com.campaignorganizer.tagging.application.port.published.TagView;
import com.campaignorganizer.tagging.domain.EntityType;
import com.campaignorganizer.whiteboard.application.port.published.WhiteboardImportPort;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarImportPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapImportPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinImportPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipImportPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventImportPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineImportPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleImportPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryImportPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldImportPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit test for the id-remapping/dispatch orchestration — every published
 * import port mocked, real Jackson for the JSON parsing under test.
 */
@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock
    private WorldImportPort worldImportPort;
    @Mock
    private CategoryImportPort categoryImportPort;
    @Mock
    private ArticleImportPort articleImportPort;
    @Mock
    private MapImportPort mapImportPort;
    @Mock
    private MapPinImportPort mapPinImportPort;
    @Mock
    private CalendarImportPort calendarImportPort;
    @Mock
    private TimelineImportPort timelineImportPort;
    @Mock
    private TimelineEventImportPort timelineEventImportPort;
    @Mock
    private RelationshipImportPort relationshipImportPort;
    @Mock
    private CampaignImportPort campaignImportPort;
    @Mock
    private PlayerImportPort playerImportPort;
    @Mock
    private CampaignPlayerImportPort campaignPlayerImportPort;
    @Mock
    private SessionImportPort sessionImportPort;
    @Mock
    private SessionAttendanceImportPort sessionAttendanceImportPort;
    @Mock
    private ArcImportPort arcImportPort;
    @Mock
    private FieldTemplateImportPort fieldTemplateImportPort;
    @Mock
    private CharacterSheetImportPort characterSheetImportPort;
    @Mock
    private DocumentImportPort documentImportPort;
    @Mock
    private StatblockImportPort statblockImportPort;
    @Mock
    private ArcBeatImportPort arcBeatImportPort;
    @Mock
    private WhiteboardImportPort whiteboardImportPort;
    @Mock
    private MediaImportPort mediaImportPort;
    @Mock
    private RollTableImportPort rollTableImportPort;
    @Mock
    private CardDeckImportPort cardDeckImportPort;
    @Mock
    private HandoutImportPort handoutImportPort;
    @Mock
    private CheatSheetImportPort cheatSheetImportPort;
    @Mock
    private TagImportPort tagImportPort;
    @Mock
    private ClockImportPort clockImportPort;
    @Mock
    private LooseThreadImportPort looseThreadImportPort;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ImportService service;

    @BeforeEach
    void setUp() {
        service = new ImportService(objectMapper, worldImportPort, categoryImportPort, articleImportPort,
                mapImportPort, mapPinImportPort, calendarImportPort, timelineImportPort,
                timelineEventImportPort, relationshipImportPort, campaignImportPort, playerImportPort,
                campaignPlayerImportPort, sessionImportPort, sessionAttendanceImportPort,
                arcImportPort, fieldTemplateImportPort, characterSheetImportPort, documentImportPort,
                statblockImportPort,
                arcBeatImportPort, whiteboardImportPort, mediaImportPort, rollTableImportPort,
                cardDeckImportPort, handoutImportPort,
                cheatSheetImportPort, tagImportPort, clockImportPort, looseThreadImportPort);
    }

    @Test
    void remapsIdsAndRewritesMediaLinksInArticleBody() throws Exception {
        UUID oldWorldId = UUID.randomUUID();
        UUID oldCategoryId = UUID.randomUUID();
        UUID oldArticleId = UUID.randomUUID();
        UUID oldChildArticleId = UUID.randomUUID();
        UUID oldMediaId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("exportVersion", ExportService.EXPORT_VERSION);
        bundle.put("world", new WorldView(oldWorldId, "Dark Caribbean", null, Map.of(), now, now));
        bundle.put("media", List.of(Map.of("id", oldMediaId, "worldId", oldWorldId, "filename", "cover.png",
                "contentType", "image/png", "sizeBytes", 3, "createdAt", now.toString())));
        bundle.put("categories", List.of(
                new CategoryView(oldCategoryId, oldWorldId, null, "Places", now, now)));
        bundle.put("articles", List.of(
                new ArticleView(oldArticleId, oldWorldId, oldCategoryId, null, "Tortuga", "tortuga",
                        "LOCATION", "See ![cover](/api/media/" + oldMediaId + "/content)", now, now),
                new ArticleView(oldChildArticleId, oldWorldId, null, oldArticleId, "Tortuga Docks",
                        "tortuga-docks", "LOCATION", "Plain.", now, now)));
        bundle.put("maps", List.of());
        bundle.put("mapPins", List.of());
        bundle.put("calendars", List.of());
        bundle.put("timelines", List.of());
        bundle.put("timelineEvents", List.of());
        bundle.put("relationships", List.of());
        bundle.put("campaigns", List.of());
        bundle.put("sessions", List.of());
        bundle.put("arcs", List.of());
        bundle.put("beats", List.of());
        bundle.put("fieldTemplates", List.of());
        bundle.put("characterSheets", List.of());
        bundle.put("statblocks", List.of());
        bundle.put("whiteboards", List.of());
        byte[] json = objectMapper.writeValueAsBytes(bundle);

        byte[] mediaBytes = {1, 2, 3};
        service.importWorld(json, Map.of(oldMediaId, mediaBytes));

        ArgumentCaptor<WorldView> worldCaptor = ArgumentCaptor.forClass(WorldView.class);
        verify(worldImportPort).importWorld(worldCaptor.capture());
        UUID newWorldId = worldCaptor.getValue().id();
        assertThat(newWorldId).isNotEqualTo(oldWorldId);
        assertThat(worldCaptor.getValue().name()).isEqualTo("Dark Caribbean");

        ArgumentCaptor<CategoryView> categoryCaptor = ArgumentCaptor.forClass(CategoryView.class);
        verify(categoryImportPort).importCategory(categoryCaptor.capture());
        UUID newCategoryId = categoryCaptor.getValue().id();
        assertThat(newCategoryId).isNotEqualTo(oldCategoryId);
        assertThat(categoryCaptor.getValue().worldId()).isEqualTo(newWorldId);

        ArgumentCaptor<ArticleView> articleCaptor = ArgumentCaptor.forClass(ArticleView.class);
        verify(articleImportPort, times(2)).importArticle(articleCaptor.capture());
        List<ArticleView> importedArticles = articleCaptor.getAllValues();
        ArticleView importedArticle = importedArticles.stream()
                .filter(a -> "Tortuga".equals(a.title())).findFirst().orElseThrow();
        ArticleView importedChild = importedArticles.stream()
                .filter(a -> "Tortuga Docks".equals(a.title())).findFirst().orElseThrow();
        assertThat(importedArticle.id()).isNotEqualTo(oldArticleId);
        assertThat(importedArticle.worldId()).isEqualTo(newWorldId);
        assertThat(importedArticle.categoryId()).isEqualTo(newCategoryId);
        assertThat(importedArticle.body()).doesNotContain(oldMediaId.toString());
        // The child's parentArticleId must follow the same remap as the parent's own id.
        assertThat(importedChild.parentArticleId()).isEqualTo(importedArticle.id());

        verify(mediaImportPort).importMedia(any(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsMismatchedExportVersion() throws Exception {
        Map<String, Object> bundle = Map.of("exportVersion", ExportService.EXPORT_VERSION + 99);
        byte[] json = objectMapper.writeValueAsBytes(bundle);

        assertThatThrownBy(() -> service.importWorld(json, Map.of()))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void remapsTagEntityIdsToTheirNewArticleOrStatblockId() throws Exception {
        UUID oldWorldId = UUID.randomUUID();
        UUID oldArticleId = UUID.randomUUID();
        UUID oldStatblockId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("exportVersion", ExportService.EXPORT_VERSION);
        bundle.put("world", new WorldView(oldWorldId, "Dark Caribbean", null, Map.of(), now, now));
        bundle.put("articles", List.of(new ArticleView(oldArticleId, oldWorldId, null, null,
                "Tortuga", "tortuga", "LOCATION", "Plain.", now, now)));
        bundle.put("statblocks", List.of(new StatblockView(oldStatblockId, oldWorldId, null, null,
                null, "Goblin", Map.of(), null, now, now)));
        bundle.put("tags", List.of(
                new TagView(UUID.randomUUID(), oldWorldId, EntityType.ARTICLE, oldArticleId, "npc", now),
                new TagView(UUID.randomUUID(), oldWorldId, EntityType.STATBLOCK, oldStatblockId, "npc",
                        now)));
        for (String key : List.of("media", "categories", "maps", "mapPins", "calendars", "timelines",
                "timelineEvents", "relationships", "campaigns", "sessions", "arcs", "beats",
                "fieldTemplates", "characterSheets", "whiteboards")) {
            bundle.put(key, List.of());
        }
        byte[] json = objectMapper.writeValueAsBytes(bundle);

        service.importWorld(json, Map.of());

        ArgumentCaptor<ArticleView> articleCaptor = ArgumentCaptor.forClass(ArticleView.class);
        verify(articleImportPort).importArticle(articleCaptor.capture());
        UUID newArticleId = articleCaptor.getValue().id();

        ArgumentCaptor<StatblockView> statblockCaptor = ArgumentCaptor.forClass(StatblockView.class);
        verify(statblockImportPort).importStatblock(statblockCaptor.capture());
        UUID newStatblockId = statblockCaptor.getValue().id();

        ArgumentCaptor<TagView> tagCaptor = ArgumentCaptor.forClass(TagView.class);
        verify(tagImportPort, times(2)).importTag(tagCaptor.capture());
        List<TagView> importedTags = tagCaptor.getAllValues();
        assertThat(importedTags)
                .extracting(TagView::entityId)
                .containsExactlyInAnyOrder(newArticleId, newStatblockId);
        assertThat(importedTags).allMatch(t -> t.name().equals("npc") && t.worldId().equals(
                articleCaptor.getValue().worldId()));
    }

    @Test
    void remapsClockCampaignIdAndKeepsSegmentsIntact() throws Exception {
        UUID oldWorldId = UUID.randomUUID();
        UUID oldCampaignId = UUID.randomUUID();
        UUID oldClockId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("exportVersion", ExportService.EXPORT_VERSION);
        bundle.put("world", new WorldView(oldWorldId, "Dark Caribbean", null, Map.of(), now, now));
        bundle.put("campaigns", List.of(
                new CampaignView(oldCampaignId, oldWorldId, "Chronicle", null, null,
                        CampaignStatus.ACTIVE, now, now)));
        bundle.put("clocks", List.of(new ClockView(oldClockId, oldCampaignId, "Doom", null,
                List.of(new ClockSegmentView(true, null, null),
                        new ClockSegmentView(false, "Alarm", "Guards notice")),
                0, now, now)));
        for (String key : List.of("media", "categories", "articles", "maps", "mapPins", "calendars",
                "timelines", "timelineEvents", "relationships", "sessions", "arcs", "beats",
                "fieldTemplates", "characterSheets", "statblocks", "whiteboards")) {
            bundle.put(key, List.of());
        }
        byte[] json = objectMapper.writeValueAsBytes(bundle);

        service.importWorld(json, Map.of());

        ArgumentCaptor<ClockView> clockCaptor = ArgumentCaptor.forClass(ClockView.class);
        verify(clockImportPort).importClock(clockCaptor.capture());
        ClockView imported = clockCaptor.getValue();
        assertThat(imported.id()).isNotEqualTo(oldClockId);
        assertThat(imported.campaignId()).isNotEqualTo(oldCampaignId);
        assertThat(imported.title()).isEqualTo("Doom");
        assertThat(imported.segments()).containsExactly(
                new ClockSegmentView(true, null, null),
                new ClockSegmentView(false, "Alarm", "Guards notice"));
    }

    @Test
    void remapsLooseThreadSessionAndCampaignIds() throws Exception {
        UUID oldWorldId = UUID.randomUUID();
        UUID oldCampaignId = UUID.randomUUID();
        UUID oldSessionId = UUID.randomUUID();
        UUID oldThreadId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("exportVersion", ExportService.EXPORT_VERSION);
        bundle.put("world", new WorldView(oldWorldId, "Dark Caribbean", null, Map.of(), now, now));
        bundle.put("campaigns", List.of(
                new CampaignView(oldCampaignId, oldWorldId, "Chronicle", null, null,
                        CampaignStatus.ACTIVE, now, now)));
        bundle.put("sessions", List.of(
                new SessionView(oldSessionId, oldCampaignId, "Session 1", 1, null, null, null, now, now)));
        bundle.put("looseThreads", List.of(new LooseThreadView(oldThreadId, oldSessionId, oldCampaignId,
                "A stranger left a coin", "OPEN", now, now)));
        for (String key : List.of("media", "categories", "articles", "maps", "mapPins", "calendars",
                "timelines", "timelineEvents", "relationships", "arcs", "beats", "clocks",
                "fieldTemplates", "characterSheets", "statblocks", "whiteboards")) {
            bundle.put(key, List.of());
        }
        byte[] json = objectMapper.writeValueAsBytes(bundle);

        service.importWorld(json, Map.of());

        ArgumentCaptor<LooseThreadView> threadCaptor = ArgumentCaptor.forClass(LooseThreadView.class);
        verify(looseThreadImportPort).importLooseThread(threadCaptor.capture());
        LooseThreadView imported = threadCaptor.getValue();
        assertThat(imported.id()).isNotEqualTo(oldThreadId);
        assertThat(imported.sessionId()).isNotEqualTo(oldSessionId);
        assertThat(imported.campaignId()).isNotEqualTo(oldCampaignId);
        assertThat(imported.text()).isEqualTo("A stranger left a coin");
        assertThat(imported.status()).isEqualTo("OPEN");
    }

    @Test
    void remapsDocumentTemplateAndCampaignIds() throws Exception {
        UUID oldWorldId = UUID.randomUUID();
        UUID oldCampaignId = UUID.randomUUID();
        UUID oldTemplateId = UUID.randomUUID();
        UUID oldDocumentId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("exportVersion", ExportService.EXPORT_VERSION);
        bundle.put("world", new WorldView(oldWorldId, "Dark Caribbean", null, Map.of(), now, now));
        bundle.put("campaigns", List.of(
                new CampaignView(oldCampaignId, oldWorldId, "Chronicle", null, null,
                        CampaignStatus.ACTIVE, now, now)));
        bundle.put("fieldTemplates", List.of(new FieldTemplateView(oldTemplateId, oldWorldId,
                "Session Zero", TemplateKind.DOCUMENT, null, List.of(), now, now)));
        bundle.put("documents", List.of(new DocumentView(oldDocumentId, oldWorldId, oldTemplateId,
                oldCampaignId, "Ashes Zero", Map.of("lines", "No animal harm"), now, now)));
        for (String key : List.of("media", "categories", "articles", "maps", "mapPins", "calendars",
                "timelines", "timelineEvents", "relationships", "sessions", "arcs", "beats", "clocks",
                "looseThreads", "characterSheets", "statblocks", "whiteboards")) {
            bundle.put(key, List.of());
        }
        byte[] json = objectMapper.writeValueAsBytes(bundle);

        service.importWorld(json, Map.of());

        ArgumentCaptor<DocumentView> documentCaptor = ArgumentCaptor.forClass(DocumentView.class);
        verify(documentImportPort).importDocument(documentCaptor.capture());
        DocumentView imported = documentCaptor.getValue();
        assertThat(imported.id()).isNotEqualTo(oldDocumentId);
        assertThat(imported.templateId()).isNotEqualTo(oldTemplateId);
        assertThat(imported.campaignId()).isNotEqualTo(oldCampaignId);
        assertThat(imported.name()).isEqualTo("Ashes Zero");
        assertThat(imported.values()).containsEntry("lines", "No animal harm");
    }

    /** Chains live in JSONB without FKs — dangling ids import as dropped refs. */
    @Test
    void dropsDanglingNestedChainIdsWhenImportingTablesAndDecks() throws Exception {
        UUID oldWorldId = UUID.randomUUID();
        UUID oldTableId = UUID.randomUUID();
        UUID danglingTableId = UUID.randomUUID();
        UUID oldDeckId = UUID.randomUUID();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("exportVersion", ExportService.EXPORT_VERSION);
        bundle.put("world", new WorldView(oldWorldId, "Dark Caribbean", null, Map.of(), now, now));
        bundle.put("rollTables", List.of(new RollTableView(oldTableId, oldWorldId, "Ambush", null,
                "1d1", 1, 1,
                List.of(new RollTableEntryView(UUID.randomUUID(), 1, 1, "Bandits",
                        List.of(danglingTableId), List.of())),
                now, now)));
        bundle.put("cardDecks", List.of(new CardDeckView(oldDeckId, oldWorldId, "Twists", null,
                List.of(new DeckCardView(UUID.randomUUID(), "Ambush", "More foes",
                        List.of(), List.of(danglingTableId))),
                now, now)));
        for (String key : List.of("media", "categories", "articles", "maps", "mapPins", "calendars",
                "timelines", "timelineEvents", "relationships", "campaigns", "sessions", "arcs",
                "beats", "fieldTemplates", "characterSheets", "statblocks", "whiteboards")) {
            bundle.put(key, List.of());
        }
        byte[] json = objectMapper.writeValueAsBytes(bundle);

        service.importWorld(json, Map.of());

        ArgumentCaptor<RollTableView> tableCaptor = ArgumentCaptor.forClass(RollTableView.class);
        verify(rollTableImportPort).importRollTable(tableCaptor.capture());
        assertThat(tableCaptor.getValue().entries().get(0).nestedTableIds()).isEmpty();

        ArgumentCaptor<CardDeckView> deckCaptor = ArgumentCaptor.forClass(CardDeckView.class);
        verify(cardDeckImportPort).importCardDeck(deckCaptor.capture());
        assertThat(deckCaptor.getValue().cards().get(0).nestedDeckIds()).isEmpty();
    }
}

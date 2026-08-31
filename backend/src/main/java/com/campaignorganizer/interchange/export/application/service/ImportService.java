package com.campaignorganizer.interchange.export.application.service;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatImportPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.arc.port.published.ArcImportPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignImportPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockImportPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadImportPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import com.campaignorganizer.campaign.application.session.port.published.SessionImportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetImportPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockImportPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateImportPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import com.campaignorganizer.interchange.export.application.port.in.ImportBackupUseCase;
import com.campaignorganizer.media.application.port.published.MediaImportPort;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetImportPort;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetView;
import com.campaignorganizer.handouts.application.port.published.HandoutImportPort;
import com.campaignorganizer.handouts.application.port.published.HandoutView;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckImportPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import com.campaignorganizer.tables.application.carddeck.port.published.DeckCardView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableEntryView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableImportPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import com.campaignorganizer.tagging.application.port.published.TagImportPort;
import com.campaignorganizer.tagging.application.port.published.TagView;
import com.campaignorganizer.whiteboard.application.port.published.WhiteboardImportPort;
import com.campaignorganizer.whiteboard.application.port.published.WhiteboardView;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarImportPort;
import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarView;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapImportPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinImportPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipImportPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventImportPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventView;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineImportPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleImportPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryImportPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryView;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldImportPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.type.CollectionType;

/**
 * Imports one world bundle as brand-new data (ADR-0061). Composes every core
 * context only through its published import port (ADR-0050); holds no core
 * domain rules of its own — mirrors {@link ExportService}.
 */
@Service
public class ImportService implements ImportBackupUseCase {

    /** Matches this app's own media-embed URLs in article bodies (see HtmlSanitizer). */
    private static final Pattern MEDIA_URL = Pattern.compile("/api/media/([0-9a-fA-F-]{36})/content");

    private final ObjectMapper objectMapper;
    private final WorldImportPort worldImportPort;
    private final CategoryImportPort categoryImportPort;
    private final ArticleImportPort articleImportPort;
    private final MapImportPort mapImportPort;
    private final MapPinImportPort mapPinImportPort;
    private final CalendarImportPort calendarImportPort;
    private final TimelineImportPort timelineImportPort;
    private final TimelineEventImportPort timelineEventImportPort;
    private final RelationshipImportPort relationshipImportPort;
    private final CampaignImportPort campaignImportPort;
    private final SessionImportPort sessionImportPort;
    private final ArcImportPort arcImportPort;
    private final FieldTemplateImportPort fieldTemplateImportPort;
    private final CharacterSheetImportPort characterSheetImportPort;
    private final StatblockImportPort statblockImportPort;
    private final ArcBeatImportPort arcBeatImportPort;
    private final WhiteboardImportPort whiteboardImportPort;
    private final MediaImportPort mediaImportPort;
    private final RollTableImportPort rollTableImportPort;
    private final CardDeckImportPort cardDeckImportPort;
    private final HandoutImportPort handoutImportPort;
    private final CheatSheetImportPort cheatSheetImportPort;
    private final TagImportPort tagImportPort;
    private final ClockImportPort clockImportPort;
    private final LooseThreadImportPort looseThreadImportPort;

    public ImportService(ObjectMapper objectMapper, WorldImportPort worldImportPort,
            CategoryImportPort categoryImportPort, ArticleImportPort articleImportPort,
            MapImportPort mapImportPort, MapPinImportPort mapPinImportPort,
            CalendarImportPort calendarImportPort, TimelineImportPort timelineImportPort,
            TimelineEventImportPort timelineEventImportPort, RelationshipImportPort relationshipImportPort,
            CampaignImportPort campaignImportPort, SessionImportPort sessionImportPort,
            ArcImportPort arcImportPort, FieldTemplateImportPort fieldTemplateImportPort,
            CharacterSheetImportPort characterSheetImportPort, StatblockImportPort statblockImportPort,
            ArcBeatImportPort arcBeatImportPort, WhiteboardImportPort whiteboardImportPort,
            MediaImportPort mediaImportPort, RollTableImportPort rollTableImportPort,
            CardDeckImportPort cardDeckImportPort, HandoutImportPort handoutImportPort,
            CheatSheetImportPort cheatSheetImportPort, TagImportPort tagImportPort,
            ClockImportPort clockImportPort, LooseThreadImportPort looseThreadImportPort) {
        this.objectMapper = objectMapper;
        this.worldImportPort = worldImportPort;
        this.categoryImportPort = categoryImportPort;
        this.articleImportPort = articleImportPort;
        this.mapImportPort = mapImportPort;
        this.mapPinImportPort = mapPinImportPort;
        this.calendarImportPort = calendarImportPort;
        this.timelineImportPort = timelineImportPort;
        this.timelineEventImportPort = timelineEventImportPort;
        this.relationshipImportPort = relationshipImportPort;
        this.campaignImportPort = campaignImportPort;
        this.sessionImportPort = sessionImportPort;
        this.arcImportPort = arcImportPort;
        this.fieldTemplateImportPort = fieldTemplateImportPort;
        this.characterSheetImportPort = characterSheetImportPort;
        this.statblockImportPort = statblockImportPort;
        this.arcBeatImportPort = arcBeatImportPort;
        this.whiteboardImportPort = whiteboardImportPort;
        this.mediaImportPort = mediaImportPort;
        this.rollTableImportPort = rollTableImportPort;
        this.cardDeckImportPort = cardDeckImportPort;
        this.handoutImportPort = handoutImportPort;
        this.cheatSheetImportPort = cheatSheetImportPort;
        this.tagImportPort = tagImportPort;
        this.clockImportPort = clockImportPort;
        this.looseThreadImportPort = looseThreadImportPort;
    }

    @Override
    @Transactional
    public void importWorld(byte[] worldBundleJson, Map<UUID, byte[]> mediaByOldId) {
        JsonNode root;
        try {
            root = objectMapper.readTree(worldBundleJson);
        } catch (JacksonException e) {
            throw new ValidationException("Malformed backup bundle");
        }
        int version = root.path("exportVersion").asInt(-1);
        if (version != ExportService.EXPORT_VERSION) {
            throw new ValidationException("Backup was made with export version " + version
                    + ", this app expects " + ExportService.EXPORT_VERSION);
        }

        JsonNode worldNode = root.get("world");
        if (worldNode == null || worldNode.isNull()) {
            throw new ValidationException("Backup bundle missing required 'world' object");
        }
        WorldView world = read(root, "world", WorldView.class);
        List<MediaBundleEntry> media = readList(root, "media", MediaBundleEntry.class);
        List<CategoryView> categories = readList(root, "categories", CategoryView.class);
        List<ArticleView> articles = readList(root, "articles", ArticleView.class);
        List<MapView> maps = readList(root, "maps", MapView.class);
        List<MapPinView> mapPins = readList(root, "mapPins", MapPinView.class);
        List<CalendarView> calendars = readList(root, "calendars", CalendarView.class);
        List<TimelineView> timelines = readList(root, "timelines", TimelineView.class);
        List<TimelineEventView> timelineEvents = readList(root, "timelineEvents", TimelineEventView.class);
        List<RelationshipView> relationships = readList(root, "relationships", RelationshipView.class);
        List<CampaignView> campaigns = readList(root, "campaigns", CampaignView.class);
        List<SessionView> sessions = readList(root, "sessions", SessionView.class);
        List<ArcView> arcs = readList(root, "arcs", ArcView.class);
        List<FieldTemplateView> fieldTemplates = readList(root, "fieldTemplates", FieldTemplateView.class);
        List<CharacterSheetView> characterSheets =
                readList(root, "characterSheets", CharacterSheetView.class);
        List<StatblockView> statblocks = readList(root, "statblocks", StatblockView.class);
        List<ArcBeatView> beats = readList(root, "beats", ArcBeatView.class);
        List<WhiteboardView> whiteboards = readList(root, "whiteboards", WhiteboardView.class);
        List<RollTableView> rollTables = readList(root, "rollTables", RollTableView.class);
        List<CardDeckView> cardDecks = readList(root, "cardDecks", CardDeckView.class);
        List<HandoutView> handouts = readList(root, "handouts", HandoutView.class);
        List<CheatSheetView> cheatSheets = readList(root, "cheatSheets", CheatSheetView.class);
        List<TagView> tags = readList(root, "tags", TagView.class);
        List<ClockView> clocks = readList(root, "clocks", ClockView.class);
        List<LooseThreadView> looseThreads = readList(root, "looseThreads", LooseThreadView.class);

        // Pass 1: every entity in the bundle gets a fresh id before anything is persisted,
        // so forward- and self-references resolve regardless of insert order.
        IdRemap remap = new IdRemap();
        remap.assign(world.id());
        media.forEach(m -> remap.assign(m.id()));
        categories.forEach(c -> remap.assign(c.id()));
        articles.forEach(a -> remap.assign(a.id()));
        maps.forEach(m -> remap.assign(m.id()));
        mapPins.forEach(p -> remap.assign(p.id()));
        calendars.forEach(c -> remap.assign(c.id()));
        timelines.forEach(t -> remap.assign(t.id()));
        timelineEvents.forEach(e -> remap.assign(e.id()));
        relationships.forEach(r -> remap.assign(r.id()));
        campaigns.forEach(c -> remap.assign(c.id()));
        sessions.forEach(s -> remap.assign(s.id()));
        arcs.forEach(a -> remap.assign(a.id()));
        fieldTemplates.forEach(f -> remap.assign(f.id()));
        characterSheets.forEach(s -> remap.assign(s.id()));
        statblocks.forEach(s -> remap.assign(s.id()));
        beats.forEach(b -> remap.assign(b.id()));
        whiteboards.forEach(w -> remap.assign(w.id()));
        rollTables.forEach(t -> remap.assign(t.id()));
        cardDecks.forEach(d -> remap.assign(d.id()));
        handouts.forEach(h -> remap.assign(h.id()));
        cheatSheets.forEach(cs -> remap.assign(cs.id()));
        tags.forEach(t -> remap.assign(t.id()));
        clocks.forEach(c -> remap.assign(c.id()));
        looseThreads.forEach(t -> remap.assign(t.id()));

        // Pass 2: persist in table-dependency order; every FK is already resolvable via remap.
        UUID newWorldId = remap.get(world.id());
        worldImportPort.importWorld(new WorldView(newWorldId, world.name(), world.description(),
                world.layerStyles(), world.createdAt(), world.updatedAt()));

        for (MediaBundleEntry m : media) {
            byte[] bytes = mediaByOldId.get(m.id());
            if (bytes != null) {
                mediaImportPort.importMedia(remap.get(m.id()), newWorldId, m.filename(), m.contentType(),
                        bytes, m.createdAt());
            }
        }

        for (CategoryView c : categories) {
            categoryImportPort.importCategory(new CategoryView(remap.get(c.id()), newWorldId,
                    remap.getOrNull(c.parentId()), c.name(), c.createdAt(), c.updatedAt()));
        }

        for (ArticleView a : articles) {
            String body = rewriteMediaLinks(a.body(), remap);
            articleImportPort.importArticle(new ArticleView(remap.get(a.id()), newWorldId,
                    remap.getOrNull(a.categoryId()), remap.getOrNull(a.parentArticleId()), a.title(),
                    a.slug(), a.template(), body, a.createdAt(), a.updatedAt()));
        }

        for (MapView m : maps) {
            mapImportPort.importMap(new MapView(remap.get(m.id()), newWorldId, m.name(),
                    remap.getOrNull(m.mediaId()), m.createdAt(), m.updatedAt()));
        }

        for (MapPinView p : mapPins) {
            mapPinImportPort.importMapPin(new MapPinView(remap.get(p.id()), remap.get(p.mapId()),
                    remap.getOrNull(p.articleId()), p.label(), p.layer(), p.x(), p.y(), p.createdAt(),
                    p.updatedAt()));
        }

        for (CalendarView c : calendars) {
            calendarImportPort.importCalendar(new CalendarView(remap.get(c.id()), newWorldId, c.name(),
                    c.daysPerWeek(), c.months(), c.createdAt(), c.updatedAt()));
        }

        for (TimelineView t : timelines) {
            timelineImportPort.importTimeline(new TimelineView(remap.get(t.id()), newWorldId, t.name(),
                    t.description(), remap.getOrNull(t.calendarId()), t.createdAt(), t.updatedAt()));
        }

        for (TimelineEventView e : timelineEvents) {
            timelineEventImportPort.importTimelineEvent(new TimelineEventView(remap.get(e.id()),
                    remap.get(e.timelineId()), remap.getOrNull(e.articleId()), e.title(), e.description(),
                    e.year(), e.month(), e.day(), e.createdAt(), e.updatedAt()));
        }

        for (RelationshipView r : relationships) {
            relationshipImportPort.importRelationship(new RelationshipView(remap.get(r.id()), newWorldId,
                    remap.get(r.fromArticleId()), remap.get(r.toArticleId()), r.label(), r.directed(),
                    r.createdAt(), r.updatedAt()));
        }

        for (CampaignView c : campaigns) {
            campaignImportPort.importCampaign(new CampaignView(remap.get(c.id()), newWorldId, c.name(),
                    c.description(), c.notes(), c.createdAt(), c.updatedAt()));
        }

        for (SessionView s : sessions) {
            sessionImportPort.importSession(new SessionView(remap.get(s.id()), remap.get(s.campaignId()),
                    s.title(), s.sessionNumber(), s.date(), s.summary(), s.notes(), s.createdAt(),
                    s.updatedAt()));
        }

        for (ArcView a : arcs) {
            arcImportPort.importArc(new ArcView(remap.get(a.id()), remap.get(a.campaignId()), a.title(),
                    a.description(), a.status(), a.position(), a.createdAt(), a.updatedAt()));
        }

        // Clocks (FR-48): campaign-scoped, no other id references inside.
        for (ClockView c : clocks) {
            clockImportPort.importClock(new ClockView(remap.get(c.id()), remap.get(c.campaignId()),
                    c.title(), c.description(), c.segments(), c.position(), c.createdAt(), c.updatedAt()));
        }

        // Loose threads (FR-49): both sessionId and campaignId are remapped.
        for (LooseThreadView t : looseThreads) {
            looseThreadImportPort.importLooseThread(new LooseThreadView(remap.get(t.id()),
                    remap.get(t.sessionId()), remap.get(t.campaignId()), t.text(), t.status(),
                    t.createdAt(), t.updatedAt()));
        }

        for (FieldTemplateView f : fieldTemplates) {
            fieldTemplateImportPort.importFieldTemplate(new FieldTemplateView(remap.get(f.id()),
                    newWorldId, f.name(), f.kind(), f.system(), f.sections(), f.createdAt(),
                    f.updatedAt()));
        }

        for (CharacterSheetView s : characterSheets) {
            characterSheetImportPort.importCharacterSheet(new CharacterSheetView(remap.get(s.id()),
                    newWorldId, remap.getOrNull(s.templateId()), remap.getOrNull(s.articleId()),
                    remap.getOrNull(s.campaignId()), s.name(), s.values(), s.createdAt(), s.updatedAt()));
        }

        for (StatblockView s : statblocks) {
            statblockImportPort.importStatblock(new StatblockView(remap.get(s.id()), newWorldId,
                    remap.getOrNull(s.articleId()), remap.getOrNull(s.campaignId()),
                    remap.getOrNull(s.templateId()), s.name(), s.stats(), s.notes(), s.createdAt(),
                    s.updatedAt()));
        }

        // Folksonomy tags (FR-47): entityId points at an already-remapped
        // article or statblock id from the same pass.
        for (TagView t : tags) {
            tagImportPort.importTag(new TagView(remap.get(t.id()), newWorldId, t.entityType(),
                    remap.get(t.entityId()), t.name(), t.createdAt()));
        }

        // Tables and decks before beats: beats reference them (FR-40). Nested
        // chains (FR-41) are rewritten to the new ids; bodies carry no other
        // id-based links.
        for (RollTableView t : rollTables) {
            rollTableImportPort.importRollTable(new RollTableView(remap.get(t.id()), newWorldId,
                    t.title(), t.description(), t.diceExpression(), t.minResult(), t.maxResult(),
                    remapEntries(t.entries(), remap), t.createdAt(), t.updatedAt()));
        }

        for (CardDeckView d : cardDecks) {
            cardDeckImportPort.importCardDeck(new CardDeckView(remap.get(d.id()), newWorldId,
                    d.title(), d.description(), remapCards(d.cards(), remap), d.createdAt(),
                    d.updatedAt()));
        }

        // Handouts (FR-46/ADR-0077): after sessions, so an optional session tag
        // remaps along with everything else.
        for (HandoutView h : handouts) {
            handoutImportPort.importHandout(new HandoutView(remap.get(h.id()), newWorldId,
                    h.title(), h.preset(), h.body(), remap.getOrNull(h.sessionId()), h.sortOrder(),
                    h.revealed(), h.createdAt(), h.updatedAt()));
        }

        // Cheat sheets (FR-37) after sessions: their session id is remapped.
        // Fragment references were validated on export and carry no ids.
        for (CheatSheetView cs : cheatSheets) {
            cheatSheetImportPort.importCheatSheet(new CheatSheetView(remap.get(cs.id()),
                    remap.get(cs.sessionId()), cs.fragments(), cs.createdAt(), cs.updatedAt()));
        }

        // Beats last among campaign data: they reference statblocks, which must exist first.
        for (ArcBeatView b : beats) {
            List<UUID> articleIds = b.articleIds().stream().map(remap::get).toList();
            List<UUID> statblockIds = b.statblockIds().stream().map(remap::get).toList();
            List<UUID> tableIds = b.tableIds() == null ? List.of() : b.tableIds().stream().map(remap::get).toList();
            List<UUID> deckIds = b.deckIds() == null ? List.of() : b.deckIds().stream().map(remap::get).toList();
            arcBeatImportPort.importArcBeat(new ArcBeatView(remap.get(b.id()), remap.get(b.arcId()),
                    b.title(), b.body(), b.done(), articleIds, statblockIds, tableIds, deckIds,
                    remap.getOrNull(b.sessionId()), b.position(), b.createdAt(), b.updatedAt()));
        }

        for (WhiteboardView w : whiteboards) {
            whiteboardImportPort.importWhiteboard(new WhiteboardView(remap.get(w.id()), newWorldId,
                    w.name(), w.nodes(), w.edges(), w.createdAt(), w.updatedAt()));
        }
    }

    /** Rewrites {@code /api/media/<id>/content} embeds to the remapped media id. */
    private String rewriteMediaLinks(String body, IdRemap remap) {
        if (body == null) {
            return null;
        }
        Matcher matcher = MEDIA_URL.matcher(body);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            UUID newId = remap.getOrNull(UUID.fromString(matcher.group(1)));
            matcher.appendReplacement(result,
                    newId == null ? matcher.group(0) : "/api/media/" + newId + "/content");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private <T> T read(JsonNode root, String field, Class<T> type) {
        return objectMapper.convertValue(root.get(field), type);
    }

    private <T> List<T> readList(JsonNode root, String field, Class<T> type) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return List.of();
        }
        CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(List.class, type);
        return objectMapper.convertValue(node, listType);
    }

    /** Rewrites the nested chains (FR-41) of imported roll-table rows to new ids. */
    private static List<RollTableEntryView> remapEntries(List<RollTableEntryView> entries,
                                                         IdRemap remap) {
        return entries == null ? List.of() : entries.stream()
                .map(e -> new RollTableEntryView(e.id(), e.minResult(), e.maxResult(), e.body(),
                        remap.all(e.nestedTableIds()), remap.all(e.nestedDeckIds())))
                .toList();
    }

    /** Rewrites the nested chains (FR-41) of imported deck cards to new ids. */
    private static List<DeckCardView> remapCards(List<DeckCardView> cards, IdRemap remap) {
        return cards == null ? List.of() : cards.stream()
                .map(c -> new DeckCardView(c.id(), c.title(), c.body(),
                        remap.all(c.nestedTableIds()), remap.all(c.nestedDeckIds())))
                .toList();
    }
}

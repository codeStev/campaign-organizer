package com.campaignorganizer.interchange.export.application.service;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatImportPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.arc.port.published.ArcImportPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindImportPort;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignImportPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerImportPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.clock.port.published.ClockImportPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockView;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterEntryView;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterImportPort;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterView;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadImportPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadView;
import com.campaignorganizer.campaign.application.player.port.published.PlayerImportPort;
import com.campaignorganizer.campaign.application.player.port.published.PlayerView;
import com.campaignorganizer.campaign.application.session.port.published.SessionAttendanceImportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionAttendanceView;
import com.campaignorganizer.campaign.application.session.port.published.SessionImportPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.application.todo.port.published.TodoImportPort;
import com.campaignorganizer.campaign.application.todo.port.published.TodoView;
import com.campaignorganizer.characters.application.document.port.published.DocumentImportPort;
import com.campaignorganizer.characters.application.document.port.published.DocumentView;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetImportPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetView;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockImportPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockImportPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateImportPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateView;
import com.campaignorganizer.characters.application.template.port.published.GameSystemImportPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemView;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateImportPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateView;
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
import java.util.HashMap;
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
    private final PlayerImportPort playerImportPort;
    private final CampaignPlayerImportPort campaignPlayerImportPort;
    private final SessionImportPort sessionImportPort;
    private final SessionAttendanceImportPort sessionAttendanceImportPort;
    private final ArcImportPort arcImportPort;
    private final BeatKindImportPort beatKindImportPort;
    private final FieldTemplateImportPort fieldTemplateImportPort;
    private final GameSystemImportPort gameSystemImportPort;
    private final GlobalFieldTemplateImportPort globalFieldTemplateImportPort;
    private final GlobalStatblockImportPort globalStatblockImportPort;
    private final CharacterSheetImportPort characterSheetImportPort;
    private final DocumentImportPort documentImportPort;
    private final StatblockImportPort statblockImportPort;
    private final EncounterImportPort encounterImportPort;
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
    private final TodoImportPort todoImportPort;

    public ImportService(ObjectMapper objectMapper, WorldImportPort worldImportPort,
            CategoryImportPort categoryImportPort, ArticleImportPort articleImportPort,
            MapImportPort mapImportPort, MapPinImportPort mapPinImportPort,
            CalendarImportPort calendarImportPort, TimelineImportPort timelineImportPort,
            TimelineEventImportPort timelineEventImportPort, RelationshipImportPort relationshipImportPort,
            CampaignImportPort campaignImportPort, PlayerImportPort playerImportPort,
            CampaignPlayerImportPort campaignPlayerImportPort, SessionImportPort sessionImportPort,
            SessionAttendanceImportPort sessionAttendanceImportPort,
            ArcImportPort arcImportPort, BeatKindImportPort beatKindImportPort,
            FieldTemplateImportPort fieldTemplateImportPort,
            GameSystemImportPort gameSystemImportPort,
            GlobalFieldTemplateImportPort globalFieldTemplateImportPort,
            GlobalStatblockImportPort globalStatblockImportPort,
            CharacterSheetImportPort characterSheetImportPort, DocumentImportPort documentImportPort,
            StatblockImportPort statblockImportPort, EncounterImportPort encounterImportPort,
            ArcBeatImportPort arcBeatImportPort, WhiteboardImportPort whiteboardImportPort,
            MediaImportPort mediaImportPort, RollTableImportPort rollTableImportPort,
            CardDeckImportPort cardDeckImportPort, HandoutImportPort handoutImportPort,
            CheatSheetImportPort cheatSheetImportPort, TagImportPort tagImportPort,
            ClockImportPort clockImportPort, LooseThreadImportPort looseThreadImportPort,
            TodoImportPort todoImportPort) {
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
        this.playerImportPort = playerImportPort;
        this.campaignPlayerImportPort = campaignPlayerImportPort;
        this.sessionImportPort = sessionImportPort;
        this.sessionAttendanceImportPort = sessionAttendanceImportPort;
        this.arcImportPort = arcImportPort;
        this.beatKindImportPort = beatKindImportPort;
        this.fieldTemplateImportPort = fieldTemplateImportPort;
        this.gameSystemImportPort = gameSystemImportPort;
        this.globalFieldTemplateImportPort = globalFieldTemplateImportPort;
        this.globalStatblockImportPort = globalStatblockImportPort;
        this.characterSheetImportPort = characterSheetImportPort;
        this.documentImportPort = documentImportPort;
        this.statblockImportPort = statblockImportPort;
        this.encounterImportPort = encounterImportPort;
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
        this.todoImportPort = todoImportPort;
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
        List<PlayerView> players = readList(root, "players", PlayerView.class);
        List<CampaignPlayerView> campaignPlayers =
                readList(root, "campaignPlayers", CampaignPlayerView.class);
        List<SessionView> sessions = readList(root, "sessions", SessionView.class);
        List<SessionAttendanceView> sessionAttendance =
                readList(root, "sessionAttendance", SessionAttendanceView.class);
        List<ArcView> arcs = readList(root, "arcs", ArcView.class);
        List<BeatKindView> beatKinds = readList(root, "beatKinds", BeatKindView.class);
        List<GameSystemView> gameSystems = readList(root, "gameSystems", GameSystemView.class);
        List<FieldTemplateView> fieldTemplates = readList(root, "fieldTemplates", FieldTemplateView.class);
        List<GlobalFieldTemplateView> globalFieldTemplates =
                readList(root, "globalFieldTemplates", GlobalFieldTemplateView.class);
        List<GlobalStatblockView> globalStatblocks =
                readList(root, "globalStatblocks", GlobalStatblockView.class);
        List<CharacterSheetView> characterSheets =
                readList(root, "characterSheets", CharacterSheetView.class);
        List<DocumentView> documents = readList(root, "documents", DocumentView.class);
        List<StatblockView> statblocks = readList(root, "statblocks", StatblockView.class);
        List<EncounterView> encounters = readList(root, "encounters", EncounterView.class);
        List<ArcBeatView> beats = readList(root, "beats", ArcBeatView.class);
        List<WhiteboardView> whiteboards = readList(root, "whiteboards", WhiteboardView.class);
        List<RollTableView> rollTables = readList(root, "rollTables", RollTableView.class);
        List<CardDeckView> cardDecks = readList(root, "cardDecks", CardDeckView.class);
        List<HandoutView> handouts = readList(root, "handouts", HandoutView.class);
        List<CheatSheetView> cheatSheets = readList(root, "cheatSheets", CheatSheetView.class);
        List<TagView> tags = readList(root, "tags", TagView.class);
        List<ClockView> clocks = readList(root, "clocks", ClockView.class);
        List<LooseThreadView> looseThreads = readList(root, "looseThreads", LooseThreadView.class);
        List<TodoView> todos = readList(root, "todos", TodoView.class);

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
        players.forEach(p -> remap.assign(p.id()));
        campaignPlayers.forEach(cp -> remap.assign(cp.id()));
        sessions.forEach(s -> remap.assign(s.id()));
        sessionAttendance.forEach(a -> remap.assign(a.id()));
        arcs.forEach(a -> remap.assign(a.id()));
        beatKinds.forEach(k -> remap.assign(k.id()));
        fieldTemplates.forEach(f -> remap.assign(f.id()));
        characterSheets.forEach(s -> remap.assign(s.id()));
        documents.forEach(d -> remap.assign(d.id()));
        statblocks.forEach(s -> remap.assign(s.id()));
        encounters.forEach(e -> remap.assign(e.id()));
        beats.forEach(b -> remap.assign(b.id()));
        whiteboards.forEach(w -> remap.assign(w.id()));
        rollTables.forEach(t -> remap.assign(t.id()));
        cardDecks.forEach(d -> remap.assign(d.id()));
        handouts.forEach(h -> remap.assign(h.id()));
        cheatSheets.forEach(cs -> remap.assign(cs.id()));
        tags.forEach(t -> remap.assign(t.id()));
        clocks.forEach(c -> remap.assign(c.id()));
        looseThreads.forEach(t -> remap.assign(t.id()));
        todos.forEach(t -> remap.assign(t.id()));

        // Pass 2: persist in table-dependency order; every FK is already resolvable via remap.
        UUID newWorldId = remap.get(world.id());
        worldImportPort.importWorld(new WorldView(newWorldId, world.name(), world.description(),
                world.layerStyles(), world.scratch(), world.createdAt(), world.updatedAt()));

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

        // Game systems (ADR-0094): resolved-or-reused by exact name, same
        // exception to the normal id-remap contract as the global template
        // catalog, and for the same reason (avoid fragmenting one shared
        // system across re-imports). Built early since campaigns (below) and
        // field templates/global templates (later) all resolve through it.
        Map<UUID, UUID> gameSystemResolution = new HashMap<>();
        for (GameSystemView sys : gameSystems) {
            GameSystemView resolved = gameSystemImportPort.importOrReuse(sys);
            gameSystemResolution.put(sys.id(), resolved.id());
        }

        for (CampaignView c : campaigns) {
            campaignImportPort.importCampaign(new CampaignView(remap.get(c.id()), newWorldId, c.name(),
                    c.description(), c.notes(), c.status(), gameSystemResolution.get(c.systemId()),
                    c.createdAt(), c.updatedAt()));
        }

        // Player pool (ADR-0091): world-scoped, no other id references inside.
        for (PlayerView p : players) {
            playerImportPort.importPlayer(new PlayerView(remap.get(p.id()), newWorldId, p.name(),
                    p.createdAt(), p.updatedAt()));
        }

        // Campaign rosters (ADR-0091): both campaignId and playerId are remapped.
        for (CampaignPlayerView cp : campaignPlayers) {
            campaignPlayerImportPort.importCampaignPlayer(new CampaignPlayerView(remap.get(cp.id()),
                    remap.get(cp.campaignId()), remap.get(cp.playerId()), cp.guest(), cp.createdAt()));
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

        // Beat kinds (FR-61, ADR-0101): world-scoped, no other id references inside.
        for (BeatKindView k : beatKinds) {
            beatKindImportPort.importBeatKind(new BeatKindView(remap.get(k.id()), newWorldId, k.name(),
                    k.color(), k.createdAt(), k.updatedAt()));
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

        // Todos (FR-54): campaignId is required, sessionId is remapped only when present.
        for (TodoView t : todos) {
            todoImportPort.importTodo(new TodoView(remap.get(t.id()), remap.get(t.campaignId()),
                    remap.getOrNull(t.sessionId()), t.text(), t.done(), t.createdAt(), t.updatedAt()));
        }

        for (FieldTemplateView f : fieldTemplates) {
            fieldTemplateImportPort.importFieldTemplate(new FieldTemplateView(remap.get(f.id()),
                    newWorldId, f.name(), f.kind(), gameSystemResolution.get(f.systemId()), f.sections(),
                    f.createdAt(), f.updatedAt()));
        }

        // Global template catalog (ADR-0093): resolved-or-reused by (kind, systemId,
        // name), not blindly recreated with a fresh id like every other entity — so
        // the id a referencing sheet/statblock should use is whatever importOrReuse
        // returns, tracked here rather than through the normal id remap.
        Map<UUID, UUID> globalTemplateResolution = new HashMap<>();
        for (GlobalFieldTemplateView g : globalFieldTemplates) {
            GlobalFieldTemplateView withResolvedSystem = new GlobalFieldTemplateView(g.id(), g.name(),
                    g.kind(), gameSystemResolution.get(g.systemId()), g.sections(), g.createdAt(),
                    g.updatedAt());
            GlobalFieldTemplateView resolved = globalFieldTemplateImportPort.importOrReuse(withResolvedSystem);
            globalTemplateResolution.put(g.id(), resolved.id());
        }

        // Global statblock catalog (ADR-0096): resolved-or-reused by (systemId,
        // name), the same exception to the normal id-remap contract as the two
        // catalogs above. Nothing downstream looks this map up — an imported
        // world statblock carries no back-reference to the catalog entry it
        // came from (copy-on-import, not a live link) — it's built purely so
        // importOrReuse runs for every catalog entry in the bundle.
        for (GlobalStatblockView g : globalStatblocks) {
            GlobalStatblockView withResolved = new GlobalStatblockView(g.id(),
                    gameSystemResolution.get(g.systemId()), globalTemplateResolution.get(g.globalTemplateId()),
                    g.name(), g.stats(), g.notes(), g.createdAt(), g.updatedAt());
            globalStatblockImportPort.importOrReuse(withResolved);
        }

        for (CharacterSheetView s : characterSheets) {
            characterSheetImportPort.importCharacterSheet(new CharacterSheetView(remap.get(s.id()),
                    newWorldId, remap.getOrNull(s.worldTemplateId()),
                    globalTemplateResolution.get(s.globalTemplateId()), remap.getOrNull(s.articleId()),
                    remap.getOrNull(s.campaignId()), s.name(), s.values(), s.createdAt(), s.updatedAt()));
        }

        // General-purpose documents (FR-50): templateId and campaignId are remapped.
        for (DocumentView d : documents) {
            documentImportPort.importDocument(new DocumentView(remap.get(d.id()), newWorldId,
                    remap.getOrNull(d.templateId()), remap.getOrNull(d.campaignId()), d.name(),
                    d.values(), d.createdAt(), d.updatedAt()));
        }

        for (StatblockView s : statblocks) {
            statblockImportPort.importStatblock(new StatblockView(remap.get(s.id()), newWorldId,
                    remap.getOrNull(s.articleId()), remap.getOrNull(s.campaignId()),
                    remap.getOrNull(s.worldTemplateId()), globalTemplateResolution.get(s.globalTemplateId()),
                    s.name(), s.stats(), s.notes(), s.createdAt(), s.updatedAt()));
        }

        // Encounters (ADR-0097): campaign-scoped, ordinary data (not resolve-or-reuse
        // like the global catalogs) - each entry's statblockId is remapped, so this
        // must come after statblocks are persisted just above.
        for (EncounterView e : encounters) {
            List<EncounterEntryView> remappedEntries = e.entries().stream()
                    .map(entry -> new EncounterEntryView(remap.get(entry.statblockId()), entry.quantity()))
                    .toList();
            encounterImportPort.importEncounter(new EncounterView(remap.get(e.id()),
                    remap.get(e.campaignId()), e.name(), e.notes(), remappedEntries, e.createdAt(),
                    e.updatedAt()));
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

        // Session attendance (ADR-0091): sessionId and playerId are remapped; a linked
        // character sheet, when present, was already imported above.
        for (SessionAttendanceView a : sessionAttendance) {
            sessionAttendanceImportPort.importAttendance(new SessionAttendanceView(remap.get(a.id()),
                    remap.get(a.sessionId()), remap.get(a.playerId()), a.present(),
                    remap.getOrNull(a.characterId()), a.createdAt()));
        }

        // Beats last among campaign data: they reference statblocks and encounters, which must exist first.
        for (ArcBeatView b : beats) {
            List<UUID> articleIds = b.articleIds().stream().map(remap::get).toList();
            List<UUID> statblockIds = b.statblockIds().stream().map(remap::get).toList();
            List<UUID> encounterIds =
                    b.encounterIds() == null ? List.of() : b.encounterIds().stream().map(remap::get).toList();
            List<UUID> tableIds = b.tableIds() == null ? List.of() : b.tableIds().stream().map(remap::get).toList();
            List<UUID> deckIds = b.deckIds() == null ? List.of() : b.deckIds().stream().map(remap::get).toList();
            arcBeatImportPort.importArcBeat(new ArcBeatView(remap.get(b.id()), remap.get(b.arcId()),
                    b.title(), b.body(), b.done(), articleIds, statblockIds, encounterIds, tableIds, deckIds,
                    remap.getOrNull(b.sessionId()), remap.getOrNull(b.kindId()), b.position(),
                    b.createdAt(), b.updatedAt()));
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

package com.campaignorganizer.interchange.export.application.service;

import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.beatkind.port.published.BeatKindQueryPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockQueryPort;
import com.campaignorganizer.campaign.application.encounter.port.published.EncounterQueryPort;
import com.campaignorganizer.campaign.application.loosethread.port.published.LooseThreadQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignPlayerQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.player.port.published.PlayerQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionAttendanceQueryPort;
import com.campaignorganizer.campaign.application.todo.port.published.TodoQueryPort;
import com.campaignorganizer.characters.application.document.port.published.DocumentQueryPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetQueryPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GameSystemQueryPort;
import com.campaignorganizer.characters.application.template.port.published.GlobalFieldTemplateQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.GlobalStatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.interchange.export.application.port.in.ExportWorldUseCase;
import com.campaignorganizer.interchange.export.application.port.in.WorldExportBundle;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.handouts.application.port.published.HandoutCategoryQueryPort;
import com.campaignorganizer.handouts.application.port.published.HandoutQueryPort;
import com.campaignorganizer.tagging.application.port.published.TagQueryPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckQueryPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import com.campaignorganizer.whiteboard.application.port.published.WhiteboardQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapCategoryQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipQueryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineEventQueryPort;
import com.campaignorganizer.worldbuilding.application.timeline.port.published.TimelineLookupPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldView;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Exports a whole world as a single JSON bundle for backup/portability (FR-22).
 * Entities are emitted flat; each child carries its parent id, so the bundle is
 * self-describing. Composes the core contexts only through their published
 * query ports (ADR-0050); holds no core domain rules of its own.
 */
@Service
public class ExportService implements ExportWorldUseCase {

    static final int EXPORT_VERSION = 1;

    private final WorldQueryPort worlds;
    private final CategoryQueryPort categories;
    private final ArticleQueryPort articles;
    private final MapCategoryQueryPort mapCategories;
    private final MapQueryPort maps;
    private final MapPinQueryPort pins;
    private final TimelineLookupPort timelines;
    private final TimelineEventQueryPort events;
    private final CalendarQueryPort calendars;
    private final RelationshipQueryPort relationships;
    private final CampaignQueryPort campaigns;
    private final PlayerQueryPort players;
    private final CampaignPlayerQueryPort campaignPlayers;
    private final SessionQueryPort sessions;
    private final SessionAttendanceQueryPort sessionAttendance;
    private final ArcQueryPort arcs;
    private final ArcBeatQueryPort beats;
    private final BeatKindQueryPort beatKinds;
    private final FieldTemplateQueryPort fieldTemplates;
    private final GameSystemQueryPort gameSystems;
    private final GlobalFieldTemplateQueryPort globalFieldTemplates;
    private final CharacterSheetQueryPort characterSheets;
    private final DocumentQueryPort documents;
    private final StatblockQueryPort statblocks;
    private final GlobalStatblockQueryPort globalStatblocks;
    private final WhiteboardQueryPort whiteboards;
    private final RollTableQueryPort rollTables;
    private final CardDeckQueryPort cardDecks;
    private final HandoutCategoryQueryPort handoutCategories;
    private final HandoutQueryPort handouts;
    private final CheatSheetQueryPort cheatSheets;
    private final TagQueryPort tags;
    private final ClockQueryPort clocks;
    private final EncounterQueryPort encounters;
    private final LooseThreadQueryPort looseThreads;
    private final TodoQueryPort todos;

    public ExportService(WorldQueryPort worlds, CategoryQueryPort categories, ArticleQueryPort articles,
                         MapCategoryQueryPort mapCategories,
                         MapQueryPort maps, MapPinQueryPort pins, TimelineLookupPort timelines,
                         TimelineEventQueryPort events, CalendarQueryPort calendars,
                         RelationshipQueryPort relationships, CampaignQueryPort campaigns,
                         PlayerQueryPort players, CampaignPlayerQueryPort campaignPlayers,
                         SessionQueryPort sessions, SessionAttendanceQueryPort sessionAttendance,
                         ArcQueryPort arcs, ArcBeatQueryPort beats, BeatKindQueryPort beatKinds,
                         FieldTemplateQueryPort fieldTemplates, GameSystemQueryPort gameSystems,
                         GlobalFieldTemplateQueryPort globalFieldTemplates,
                         CharacterSheetQueryPort characterSheets,
                         DocumentQueryPort documents, StatblockQueryPort statblocks,
                         GlobalStatblockQueryPort globalStatblocks,
                         WhiteboardQueryPort whiteboards,
                         RollTableQueryPort rollTables, CardDeckQueryPort cardDecks,
                         HandoutCategoryQueryPort handoutCategories, HandoutQueryPort handouts,
                         CheatSheetQueryPort cheatSheets,
                         TagQueryPort tags, ClockQueryPort clocks, EncounterQueryPort encounters,
                         LooseThreadQueryPort looseThreads, TodoQueryPort todos) {
        this.worlds = worlds;
        this.categories = categories;
        this.articles = articles;
        this.mapCategories = mapCategories;
        this.maps = maps;
        this.pins = pins;
        this.timelines = timelines;
        this.events = events;
        this.calendars = calendars;
        this.relationships = relationships;
        this.campaigns = campaigns;
        this.players = players;
        this.campaignPlayers = campaignPlayers;
        this.sessions = sessions;
        this.sessionAttendance = sessionAttendance;
        this.arcs = arcs;
        this.beats = beats;
        this.beatKinds = beatKinds;
        this.fieldTemplates = fieldTemplates;
        this.gameSystems = gameSystems;
        this.globalFieldTemplates = globalFieldTemplates;
        this.characterSheets = characterSheets;
        this.documents = documents;
        this.statblocks = statblocks;
        this.globalStatblocks = globalStatblocks;
        this.whiteboards = whiteboards;
        this.rollTables = rollTables;
        this.cardDecks = cardDecks;
        this.handoutCategories = handoutCategories;
        this.handouts = handouts;
        this.cheatSheets = cheatSheets;
        this.tags = tags;
        this.clocks = clocks;
        this.encounters = encounters;
        this.looseThreads = looseThreads;
        this.todos = todos;
    }

    @Override
    @Transactional(readOnly = true)
    public WorldExportBundle export(UUID worldId) {
        WorldView world = worlds.findById(worldId)
                .orElseThrow(() -> new NotFoundException("World not found"));

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("exportVersion", EXPORT_VERSION);
        bundle.put("exportedAt", Instant.now().toString());
        bundle.put("world", world);
        bundle.put("categories", categories.findByWorld(worldId));
        bundle.put("articles", articles.findByWorld(worldId));
        bundle.put("mapCategories", mapCategories.findByWorld(worldId));

        List<Object> allPins = new ArrayList<>();
        List<MapView> worldMaps = maps.findByWorld(worldId);
        worldMaps.forEach(m -> allPins.addAll(pins.findByMap(m.id())));
        bundle.put("maps", worldMaps);
        bundle.put("mapPins", allPins);

        var worldTimelines = timelines.findByWorld(worldId);
        List<Object> allEvents = new ArrayList<>();
        worldTimelines.forEach(t -> allEvents.addAll(events.findByTimeline(t.id())));
        bundle.put("timelines", worldTimelines);
        bundle.put("timelineEvents", allEvents);

        // Calendars now carry their months inline (via the worldbuilding query port).
        bundle.put("calendars", calendars.findByWorld(worldId));

        bundle.put("relationships", relationships.findByWorld(worldId));

        var worldCampaigns = campaigns.findByWorld(worldId);
        List<Object> allSessions = new ArrayList<>();
        List<Object> allArcs = new ArrayList<>();
        List<Object> allBeats = new ArrayList<>();
        List<Object> allClocks = new ArrayList<>();
        List<Object> allEncounters = new ArrayList<>();
        List<Object> allLooseThreads = new ArrayList<>();
        // Campaign rosters (ADR-0091): campaign-scoped membership rows.
        List<Object> allCampaignPlayers = new ArrayList<>();
        // Todos (FR-54): standing and session-attached rows, campaign-scoped.
        List<Object> allTodos = new ArrayList<>();
        worldCampaigns.forEach(c -> {
            allSessions.addAll(sessions.findOrdered(c.id()));
            arcs.findByCampaign(c.id()).forEach(a -> {
                allArcs.add(a);
                allBeats.addAll(beats.findByArc(a.id()));
            });
            // Clocks (FR-48): campaign-scoped, no beat linkage to walk.
            allClocks.addAll(clocks.findByCampaign(c.id()));
            // Encounters (ADR-0097): campaign-scoped; beats link to them by id.
            allEncounters.addAll(encounters.findByCampaign(c.id()));
            // Loose threads (FR-49): campaign-scoped via the denormalized column.
            allLooseThreads.addAll(looseThreads.findByCampaign(c.id()));
            allCampaignPlayers.addAll(campaignPlayers.findByCampaign(c.id()));
            allTodos.addAll(todos.findByCampaign(c.id()));
        });
        bundle.put("campaigns", worldCampaigns);
        bundle.put("sessions", allSessions);
        bundle.put("arcs", allArcs);
        bundle.put("beats", allBeats);
        bundle.put("clocks", allClocks);
        bundle.put("encounters", allEncounters);
        bundle.put("looseThreads", allLooseThreads);
        bundle.put("todos", allTodos);
        // Player pool (ADR-0091): world-scoped, reused across the world's campaigns.
        bundle.put("players", players.findByWorld(worldId));
        bundle.put("campaignPlayers", allCampaignPlayers);
        // Beat kinds (FR-61, ADR-0101): world-scoped, reused across the world's arcs/beats.
        bundle.put("beatKinds", beatKinds.findByWorld(worldId));
        // Session cheat sheets (FR-37): one per session, when present.
        List<CheatSheetView> cheatSheetViews = new ArrayList<>();
        // Session attendance (ADR-0091): zero or more rows per session.
        List<Object> allAttendance = new ArrayList<>();
        for (Object s : allSessions) {
            UUID sessionId = ((SessionView) s).id();
            cheatSheets.findBySession(sessionId).ifPresent(cheatSheetViews::add);
            allAttendance.addAll(sessionAttendance.findBySession(sessionId));
        }
        bundle.put("cheatSheets", cheatSheetViews);
        bundle.put("sessionAttendance", allAttendance);

        bundle.put("fieldTemplates", fieldTemplates.findByWorld(worldId));
        // Game systems (ADR-0094) and the global template catalog (ADR-0093):
        // neither is world-scoped, but both are included so a fresh instance
        // importing this bundle has whatever's referenced; import
        // resolves-or-reuses each by name/(kind, systemId, name) rather than
        // blindly recreating, so re-importing never duplicates them.
        bundle.put("gameSystems", gameSystems.findAll());
        bundle.put("globalFieldTemplates", globalFieldTemplates.findAll());
        // Global statblock catalog (ADR-0096): same "not world-scoped but
        // shipped anyway, resolved-or-reused on import" treatment as the two
        // catalogs above.
        bundle.put("globalStatblocks", globalStatblocks.findAll());
        bundle.put("characterSheets", characterSheets.findByWorld(worldId));
        // General-purpose documents (FR-50).
        bundle.put("documents", documents.findByWorld(worldId));
        bundle.put("statblocks", statblocks.findByWorld(worldId));
        bundle.put("whiteboards", whiteboards.findByWorld(worldId));
        // Randomizers (FR-40): beats reference them, so they ship with the world.
        bundle.put("rollTables", rollTables.findByWorld(worldId));
        bundle.put("cardDecks", cardDecks.findByWorld(worldId));
        // Player-facing props (FR-46).
        bundle.put("handoutCategories", handoutCategories.findByWorld(worldId));
        bundle.put("handouts", handouts.findByWorld(worldId));
        // Folksonomy tags on articles/statblocks (FR-47).
        bundle.put("tags", tags.findByWorld(worldId));

        return new WorldExportBundle(world.name(), bundle);
    }
}

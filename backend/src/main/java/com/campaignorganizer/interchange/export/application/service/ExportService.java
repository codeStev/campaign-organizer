package com.campaignorganizer.interchange.export.application.service;

import com.campaignorganizer.worldbuilding.application.calendar.port.published.CalendarQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.clock.port.published.ClockQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.CheatSheetView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.characters.application.sheet.port.published.CharacterSheetQueryPort;
import com.campaignorganizer.characters.application.template.port.published.FieldTemplateQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.interchange.export.application.port.in.ExportWorldUseCase;
import com.campaignorganizer.interchange.export.application.port.in.WorldExportBundle;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.handouts.application.port.published.HandoutQueryPort;
import com.campaignorganizer.tagging.application.port.published.TagQueryPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckQueryPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import com.campaignorganizer.whiteboard.application.port.published.WhiteboardQueryPort;
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
    private final MapQueryPort maps;
    private final MapPinQueryPort pins;
    private final TimelineLookupPort timelines;
    private final TimelineEventQueryPort events;
    private final CalendarQueryPort calendars;
    private final RelationshipQueryPort relationships;
    private final CampaignQueryPort campaigns;
    private final SessionQueryPort sessions;
    private final ArcQueryPort arcs;
    private final ArcBeatQueryPort beats;
    private final FieldTemplateQueryPort fieldTemplates;
    private final CharacterSheetQueryPort characterSheets;
    private final StatblockQueryPort statblocks;
    private final WhiteboardQueryPort whiteboards;
    private final RollTableQueryPort rollTables;
    private final CardDeckQueryPort cardDecks;
    private final HandoutQueryPort handouts;
    private final CheatSheetQueryPort cheatSheets;
    private final TagQueryPort tags;
    private final ClockQueryPort clocks;

    public ExportService(WorldQueryPort worlds, CategoryQueryPort categories, ArticleQueryPort articles,
                         MapQueryPort maps, MapPinQueryPort pins, TimelineLookupPort timelines,
                         TimelineEventQueryPort events, CalendarQueryPort calendars,
                         RelationshipQueryPort relationships, CampaignQueryPort campaigns,
                         SessionQueryPort sessions, ArcQueryPort arcs, ArcBeatQueryPort beats,
                         FieldTemplateQueryPort fieldTemplates, CharacterSheetQueryPort characterSheets,
                         StatblockQueryPort statblocks, WhiteboardQueryPort whiteboards,
                         RollTableQueryPort rollTables, CardDeckQueryPort cardDecks,
                         HandoutQueryPort handouts, CheatSheetQueryPort cheatSheets,
                         TagQueryPort tags, ClockQueryPort clocks) {
        this.worlds = worlds;
        this.categories = categories;
        this.articles = articles;
        this.maps = maps;
        this.pins = pins;
        this.timelines = timelines;
        this.events = events;
        this.calendars = calendars;
        this.relationships = relationships;
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.arcs = arcs;
        this.beats = beats;
        this.fieldTemplates = fieldTemplates;
        this.characterSheets = characterSheets;
        this.statblocks = statblocks;
        this.whiteboards = whiteboards;
        this.rollTables = rollTables;
        this.cardDecks = cardDecks;
        this.handouts = handouts;
        this.cheatSheets = cheatSheets;
        this.tags = tags;
        this.clocks = clocks;
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
        worldCampaigns.forEach(c -> {
            allSessions.addAll(sessions.findOrdered(c.id()));
            arcs.findByCampaign(c.id()).forEach(a -> {
                allArcs.add(a);
                allBeats.addAll(beats.findByArc(a.id()));
            });
            // Clocks (FR-48): campaign-scoped, no beat linkage to walk.
            allClocks.addAll(clocks.findByCampaign(c.id()));
        });
        bundle.put("campaigns", worldCampaigns);
        bundle.put("sessions", allSessions);
        bundle.put("arcs", allArcs);
        bundle.put("beats", allBeats);
        bundle.put("clocks", allClocks);
        // Session cheat sheets (FR-37): one per session, when present.
        List<CheatSheetView> cheatSheetViews = new ArrayList<>();
        for (Object s : allSessions) {
            cheatSheets.findBySession(((SessionView) s).id()).ifPresent(cheatSheetViews::add);
        }
        bundle.put("cheatSheets", cheatSheetViews);

        bundle.put("fieldTemplates", fieldTemplates.findByWorld(worldId));
        bundle.put("characterSheets", characterSheets.findByWorld(worldId));
        bundle.put("statblocks", statblocks.findByWorld(worldId));
        bundle.put("whiteboards", whiteboards.findByWorld(worldId));
        // Randomizers (FR-40): beats reference them, so they ship with the world.
        bundle.put("rollTables", rollTables.findByWorld(worldId));
        bundle.put("cardDecks", cardDecks.findByWorld(worldId));
        // Player-facing props (FR-46).
        bundle.put("handouts", handouts.findByWorld(worldId));
        // Folksonomy tags on articles/statblocks (FR-47).
        bundle.put("tags", tags.findByWorld(worldId));

        return new WorldExportBundle(world.name(), bundle);
    }
}

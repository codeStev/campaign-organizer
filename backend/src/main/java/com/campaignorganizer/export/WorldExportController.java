package com.campaignorganizer.export;

import com.campaignorganizer.calendar.CalendarMonthRepository;
import com.campaignorganizer.calendar.CalendarRepository;
import com.campaignorganizer.campaign.ArcBeatRepository;
import com.campaignorganizer.campaign.ArcRepository;
import com.campaignorganizer.campaign.CampaignRepository;
import com.campaignorganizer.campaign.SessionRepository;
import com.campaignorganizer.map.MapPinRepository;
import com.campaignorganizer.map.WorldMap;
import com.campaignorganizer.map.WorldMapRepository;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipQueryPort;
import com.campaignorganizer.sheet.CharacterSheetRepository;
import com.campaignorganizer.sheet.SheetTemplateRepository;
import com.campaignorganizer.statblock.StatblockRepository;
import com.campaignorganizer.timeline.TimelineRepository;
import com.campaignorganizer.timeline.TimelineEventRepository;
import com.campaignorganizer.whiteboard.adapter.out.persistence.WhiteboardJpaRepository;
import com.campaignorganizer.wiki.ArticleRepository;
import com.campaignorganizer.wiki.CategoryRepository;
import com.campaignorganizer.world.World;
import com.campaignorganizer.world.WorldRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Exports a whole world as a single JSON bundle for backup/portability (FR-22).
 * Entities are emitted flat; each child carries its parent id, so the bundle is
 * self-describing.
 */
@RestController
@RequestMapping("/api/worlds/{worldId}/export")
public class WorldExportController {

    static final int EXPORT_VERSION = 1;

    private final WorldRepository worlds;
    private final CategoryRepository categories;
    private final ArticleRepository articles;
    private final WorldMapRepository maps;
    private final MapPinRepository pins;
    private final TimelineRepository timelines;
    private final TimelineEventRepository events;
    private final CalendarRepository calendars;
    private final CalendarMonthRepository calendarMonths;
    private final RelationshipQueryPort relationships;
    private final CampaignRepository campaigns;
    private final SessionRepository sessions;
    private final ArcRepository arcs;
    private final ArcBeatRepository beats;
    private final SheetTemplateRepository sheetTemplates;
    private final CharacterSheetRepository characterSheets;
    private final StatblockRepository statblocks;
    private final WhiteboardJpaRepository whiteboards;

    public WorldExportController(WorldRepository worlds, CategoryRepository categories,
                                ArticleRepository articles, WorldMapRepository maps, MapPinRepository pins,
                                TimelineRepository timelines, TimelineEventRepository events,
                                CalendarRepository calendars, CalendarMonthRepository calendarMonths,
                                RelationshipQueryPort relationships, CampaignRepository campaigns,
                                SessionRepository sessions, ArcRepository arcs, ArcBeatRepository beats,
                                SheetTemplateRepository sheetTemplates,
                                CharacterSheetRepository characterSheets, StatblockRepository statblocks,
                                WhiteboardJpaRepository whiteboards) {
        this.worlds = worlds;
        this.categories = categories;
        this.articles = articles;
        this.maps = maps;
        this.pins = pins;
        this.timelines = timelines;
        this.events = events;
        this.calendars = calendars;
        this.calendarMonths = calendarMonths;
        this.relationships = relationships;
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.arcs = arcs;
        this.beats = beats;
        this.sheetTemplates = sheetTemplates;
        this.characterSheets = characterSheets;
        this.statblocks = statblocks;
        this.whiteboards = whiteboards;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> export(@PathVariable UUID worldId) {
        World world = worlds.findById(worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found"));

        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("exportVersion", EXPORT_VERSION);
        bundle.put("exportedAt", Instant.now().toString());
        bundle.put("world", world);
        bundle.put("categories", categories.findByWorldIdOrderByNameAsc(worldId));
        bundle.put("articles", articles.findByWorldIdOrderByCreatedAtDesc(worldId));

        List<Object> allPins = new ArrayList<>();
        List<WorldMap> worldMaps = maps.findByWorldIdOrderByCreatedAtDesc(worldId);
        worldMaps.forEach(m -> allPins.addAll(pins.findByMapIdOrderByCreatedAtAsc(m.getId())));
        bundle.put("maps", worldMaps);
        bundle.put("mapPins", allPins);

        var worldTimelines = timelines.findByWorldIdOrderByCreatedAtDesc(worldId);
        List<Object> allEvents = new ArrayList<>();
        worldTimelines.forEach(t -> allEvents.addAll(events.findOrdered(t.getId())));
        bundle.put("timelines", worldTimelines);
        bundle.put("timelineEvents", allEvents);

        var worldCalendars = calendars.findByWorldIdOrderByCreatedAtDesc(worldId);
        List<Object> allMonths = new ArrayList<>();
        worldCalendars.forEach(c -> allMonths.addAll(calendarMonths.findByCalendarIdOrderByPositionAsc(c.getId())));
        bundle.put("calendars", worldCalendars);
        bundle.put("calendarMonths", allMonths);

        bundle.put("relationships", relationships.findByWorld(worldId));

        var worldCampaigns = campaigns.findByWorldIdOrderByCreatedAtDesc(worldId);
        List<Object> allSessions = new ArrayList<>();
        List<Object> allArcs = new ArrayList<>();
        List<Object> allBeats = new ArrayList<>();
        worldCampaigns.forEach(c -> {
            allSessions.addAll(sessions.findOrdered(c.getId()));
            arcs.findByCampaignIdOrderByPositionAscCreatedAtAsc(c.getId()).forEach(a -> {
                allArcs.add(a);
                allBeats.addAll(beats.findByArcIdOrderByPositionAscCreatedAtAsc(a.getId()));
            });
        });
        bundle.put("campaigns", worldCampaigns);
        bundle.put("sessions", allSessions);
        bundle.put("arcs", allArcs);
        bundle.put("beats", allBeats);

        bundle.put("sheetTemplates", sheetTemplates.findByWorldIdOrderByCreatedAtDesc(worldId));
        bundle.put("characterSheets", characterSheets.findByWorldIdOrderByCreatedAtDesc(worldId));
        bundle.put("statblocks", statblocks.findByWorldIdOrderByCreatedAtDesc(worldId));
        bundle.put("whiteboards", whiteboards.findByWorldIdOrderByCreatedAtDesc(worldId));

        String filename = "world-" + slug(world.getName()) + ".json";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .body(bundle);
    }

    private static String slug(String name) {
        String s = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
        return s.isEmpty() ? "export" : s;
    }
}

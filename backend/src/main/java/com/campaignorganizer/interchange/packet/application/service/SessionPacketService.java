package com.campaignorganizer.interchange.packet.application.service;

import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.interchange.packet.application.port.in.BuildSessionPacketUseCase;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.PacketArticle;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.PacketBeat;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.PacketCardDeck;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.PacketDeckCard;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.PacketMap;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.PacketPin;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.PacketRollTable;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.PacketRollTableEntry;
import com.campaignorganizer.interchange.packet.application.port.in.SessionPacketDtos.SessionPacketResponse;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckQueryPort;
import com.campaignorganizer.tables.application.carddeck.port.published.CardDeckView;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableQueryPort;
import com.campaignorganizer.tables.application.rolltable.port.published.RollTableView;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Assembles everything needed to run one session onto a single printable
 * packet (ADR-0036). Composes the core contexts only through their published
 * query ports (ADR-0050); holds no core domain rules of its own.
 */
@Service
public class SessionPacketService implements BuildSessionPacketUseCase {

    private final CampaignQueryPort campaigns;
    private final SessionQueryPort sessions;
    private final ArcQueryPort arcs;
    private final ArcBeatQueryPort beats;
    private final ArticleQueryPort articles;
    private final ArticleRenderPort articleRenderer;
    private final StatblockQueryPort statblocks;
    private final RollTableQueryPort rollTables;
    private final CardDeckQueryPort cardDecks;
    private final MapQueryPort maps;
    private final MapPinQueryPort pins;

    public SessionPacketService(CampaignQueryPort campaigns, SessionQueryPort sessions,
                                ArcQueryPort arcs, ArcBeatQueryPort beats,
                                ArticleQueryPort articles, ArticleRenderPort articleRenderer,
                                StatblockQueryPort statblocks, RollTableQueryPort rollTables,
                                CardDeckQueryPort cardDecks, MapQueryPort maps, MapPinQueryPort pins) {
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.arcs = arcs;
        this.beats = beats;
        this.articles = articles;
        this.articleRenderer = articleRenderer;
        this.statblocks = statblocks;
        this.rollTables = rollTables;
        this.cardDecks = cardDecks;
        this.maps = maps;
        this.pins = pins;
    }

    @Override
    @Transactional(readOnly = true)
    public SessionPacketResponse packet(UUID worldId, UUID campaignId, UUID sessionId) {
        CampaignView campaign = campaigns.findByIdInWorld(campaignId, worldId)
                .orElseThrow(() -> new NotFoundException("Campaign not found"));
        SessionView session = sessions.findByIdInCampaign(sessionId, campaignId)
                .orElseThrow(() -> new NotFoundException("Session not found"));

        List<ArcBeatView> sessionBeats = beats.findBySession(sessionId);

        Map<UUID, String> arcTitles = arcs.findByCampaign(campaignId)
                .stream().collect(Collectors.toMap(ArcView::id, ArcView::title));

        // Articles referenced by the session's beats, first-seen order preserved.
        LinkedHashSet<UUID> articleIds = sessionBeats.stream()
                .flatMap(b -> b.articleIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<PacketBeat> packetBeats = sessionBeats.stream()
                .map(b -> new PacketBeat(b.id(), b.title(), b.body(), b.done(),
                        arcTitles.get(b.arcId()), List.copyOf(b.articleIds())))
                .toList();

        // Statblocks referenced by this session's beats first, then campaign-scoped extras.
        Map<UUID, StatblockView> sbById = new LinkedHashMap<>();
        sessionBeats.stream().flatMap(b -> b.statblockIds().stream()).forEach(id -> {
            if (!sbById.containsKey(id)) {
                statblocks.findByIdInWorld(id, worldId).ifPresent(s -> sbById.put(id, s));
            }
        });
        statblocks.findByWorldAndCampaign(worldId, campaignId)
                .forEach(s -> sbById.putIfAbsent(s.id(), s));
        List<StatblockView> packetStatblocks = List.copyOf(sbById.values());

        // Tables and decks referenced by the session's beats (FR-40), then
        // everything their rows/cards chain in (FR-41), first-seen order. The
        // visited sets double as cycle detection: a chained loop is printed
        // once and never walked again.
        LinkedHashSet<UUID> tableIds = sessionBeats.stream()
                .flatMap(b -> b.tableIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        LinkedHashSet<UUID> deckIds = sessionBeats.stream()
                .flatMap(b -> b.deckIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<UUID, RollTableView> tablesById = new LinkedHashMap<>();
        Map<UUID, CardDeckView> decksById = new LinkedHashMap<>();
        Deque<UUID> tableQueue = new ArrayDeque<>(tableIds);
        Deque<UUID> deckQueue = new ArrayDeque<>(deckIds);
        while (!tableQueue.isEmpty() || !deckQueue.isEmpty()) {
            UUID tableId = tableQueue.poll();
            if (tableId != null && !tablesById.containsKey(tableId)) {
                rollTables.findByIdInWorld(tableId, worldId).ifPresent(t -> {
                    tablesById.put(tableId, t);
                    t.entries().forEach(e -> {
                        tableQueue.addAll(e.nestedTableIds());
                        deckQueue.addAll(e.nestedDeckIds());
                    });
                });
            }
            UUID deckId = deckQueue.poll();
            if (deckId != null && !decksById.containsKey(deckId)) {
                cardDecks.findByIdInWorld(deckId, worldId).ifPresent(d -> {
                    decksById.put(deckId, d);
                    d.cards().forEach(c -> {
                        tableQueue.addAll(c.nestedTableIds());
                        deckQueue.addAll(c.nestedDeckIds());
                    });
                });
            }
        }
        List<RollTableView> tables = List.copyOf(tablesById.values());
        List<CardDeckView> decks = List.copyOf(decksById.values());

        // Global print-once rule: articles referenced from roll-table outcomes
        // or deck cards join the same articles section — each id prints once,
        // in first-seen order (beats before tables before decks).
        LinkedHashSet<String> refNames = new LinkedHashSet<>();
        tables.forEach(t -> t.entries().forEach(e -> refNames.addAll(articleRenderer.linkTargets(e.body()))));
        decks.forEach(d -> d.cards().forEach(c -> refNames.addAll(articleRenderer.linkTargets(c.body()))));
        articleIds.addAll(articles.resolveRefs(worldId, refNames).values());

        List<PacketArticle> packetArticles = articleIds.stream()
                .map(id -> articles.findByIdInWorld(id, worldId).orElse(null))
                .filter(a -> a != null)
                .map(this::toPacketArticle)
                .toList();

        // Maps reachable from the session: any map with a pin linking a packet article
        // (beat links and outcome/card references alike).
        LinkedHashSet<UUID> mapIds = new LinkedHashSet<>();
        articleIds.forEach(artId -> pins.findByArticle(artId).forEach(p -> mapIds.add(p.mapId())));
        List<PacketMap> packetMaps = mapIds.stream()
                .map(id -> maps.findByIdInWorld(id, worldId).orElse(null))
                .filter(m -> m != null)
                .map(this::toPacketMap)
                .toList();

        List<PacketRollTable> packetTables = tables.stream().map(this::toPacketRollTable).toList();
        List<PacketCardDeck> packetDecks = decks.stream().map(this::toPacketCardDeck).toList();

        return new SessionPacketResponse(session, campaign.name(),
                packetBeats, packetArticles, packetMaps, packetStatblocks, packetTables, packetDecks);
    }

    /** Entry outcome bodies go through the same render pipeline as article bodies. */
    private PacketRollTable toPacketRollTable(RollTableView t) {
        List<PacketRollTableEntry> entries = t.entries().stream()
                .map(e -> new PacketRollTableEntry(e.minResult(), e.maxResult(),
                        articleRenderer.renderBody(t.worldId(), e.body())))
                .toList();
        return new PacketRollTable(t.id(), t.title(), t.diceExpression(), t.minResult(), t.maxResult(),
                entries);
    }

    private PacketCardDeck toPacketCardDeck(CardDeckView d) {
        List<PacketDeckCard> cards = d.cards().stream()
                .map(c -> new PacketDeckCard(c.title(), articleRenderer.renderBody(d.worldId(), c.body())))
                .toList();
        return new PacketCardDeck(d.id(), d.title(), cards);
    }

    private PacketMap toPacketMap(MapView map) {
        String imageUrl = map.mediaId() == null ? null : "/api/media/" + map.mediaId() + "/content";
        List<PacketPin> packetPins = pins.findByMap(map.id()).stream()
                .map(this::toPacketPin)
                .toList();
        return new PacketMap(map.id(), map.name(), imageUrl, packetPins);
    }

    /** Resolve a pin's label: its own, else the linked article's title. */
    private PacketPin toPacketPin(MapPinView pin) {
        String label = pin.label();
        if ((label == null || label.isBlank()) && pin.articleId() != null) {
            label = articles.findById(pin.articleId()).map(ArticleView::title).orElse(null);
        }
        return new PacketPin(pin.x(), pin.y(), label);
    }

    private PacketArticle toPacketArticle(ArticleView a) {
        return new PacketArticle(a.id(), a.title(), a.template(),
                articleRenderer.renderBody(a.worldId(), a.body()));
    }
}

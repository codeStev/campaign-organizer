package com.campaignorganizer.campaign;

import com.campaignorganizer.campaign.SessionPacketDtos.PacketArticle;
import com.campaignorganizer.campaign.SessionPacketDtos.PacketBeat;
import com.campaignorganizer.campaign.SessionPacketDtos.PacketMap;
import com.campaignorganizer.campaign.SessionPacketDtos.PacketPin;
import com.campaignorganizer.campaign.SessionPacketDtos.SessionPacketResponse;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcBeatView;
import com.campaignorganizer.campaign.application.arc.port.published.ArcQueryPort;
import com.campaignorganizer.campaign.application.arc.port.published.ArcView;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignView;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionView;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapPinView;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapQueryPort;
import com.campaignorganizer.worldbuilding.application.map.port.published.MapView;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRenderPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Assembles everything needed to run one session onto a single printable packet (ADR-0036). */
@Service
public class SessionPacketService {

    private final CampaignQueryPort campaigns;
    private final SessionQueryPort sessions;
    private final ArcQueryPort arcs;
    private final ArcBeatQueryPort beats;
    private final ArticleQueryPort articles;
    private final ArticleRenderPort articleRenderer;
    private final StatblockQueryPort statblocks;
    private final MapQueryPort maps;
    private final MapPinQueryPort pins;

    public SessionPacketService(CampaignQueryPort campaigns, SessionQueryPort sessions,
                                ArcQueryPort arcs, ArcBeatQueryPort beats,
                                ArticleQueryPort articles, ArticleRenderPort articleRenderer,
                                StatblockQueryPort statblocks, MapQueryPort maps, MapPinQueryPort pins) {
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.arcs = arcs;
        this.beats = beats;
        this.articles = articles;
        this.articleRenderer = articleRenderer;
        this.statblocks = statblocks;
        this.maps = maps;
        this.pins = pins;
    }

    public SessionPacketResponse packet(UUID worldId, UUID campaignId, UUID sessionId) {
        CampaignView campaign = campaigns.findByIdInWorld(campaignId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        SessionView session = sessions.findByIdInCampaign(sessionId, campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        List<ArcBeatView> sessionBeats = beats.findBySession(sessionId);

        Map<UUID, String> arcTitles = arcs.findByCampaign(campaignId)
                .stream().collect(Collectors.toMap(ArcView::id, ArcView::title));

        // Articles referenced by the session's beats, first-seen order preserved.
        LinkedHashSet<UUID> articleIds = sessionBeats.stream()
                .flatMap(b -> b.articleIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<PacketArticle> packetArticles = articleIds.stream()
                .map(id -> articles.findByIdInWorld(id, worldId).orElse(null))
                .filter(a -> a != null)
                .map(this::toPacketArticle)
                .toList();

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

        // Maps reachable from the session: any map with a pin linking a beat article.
        LinkedHashSet<UUID> mapIds = new LinkedHashSet<>();
        articleIds.forEach(artId -> pins.findByArticle(artId).forEach(p -> mapIds.add(p.mapId())));
        List<PacketMap> packetMaps = mapIds.stream()
                .map(id -> maps.findByIdInWorld(id, worldId).orElse(null))
                .filter(m -> m != null)
                .map(this::toPacketMap)
                .toList();

        return new SessionPacketResponse(session, campaign.name(),
                packetBeats, packetArticles, packetMaps, packetStatblocks);
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

package com.campaignorganizer.campaign;

import com.campaignorganizer.campaign.SessionDtos.SessionResponse;
import com.campaignorganizer.campaign.SessionPacketDtos.PacketArticle;
import com.campaignorganizer.campaign.SessionPacketDtos.PacketBeat;
import com.campaignorganizer.campaign.SessionPacketDtos.SessionPacketResponse;
import com.campaignorganizer.statblock.Statblock;
import com.campaignorganizer.statblock.StatblockDtos.StatblockResponse;
import com.campaignorganizer.statblock.StatblockRepository;
import com.campaignorganizer.wiki.Article;
import com.campaignorganizer.wiki.ArticleRepository;
import com.campaignorganizer.wiki.AutoLinker;
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

    private final CampaignRepository campaigns;
    private final SessionRepository sessions;
    private final ArcRepository arcs;
    private final ArcBeatRepository beats;
    private final ArticleRepository articles;
    private final StatblockRepository statblocks;
    private final AutoLinker autoLinker;

    public SessionPacketService(CampaignRepository campaigns, SessionRepository sessions,
                                ArcRepository arcs, ArcBeatRepository beats,
                                ArticleRepository articles, StatblockRepository statblocks,
                                AutoLinker autoLinker) {
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.arcs = arcs;
        this.beats = beats;
        this.articles = articles;
        this.statblocks = statblocks;
        this.autoLinker = autoLinker;
    }

    public SessionPacketResponse packet(UUID worldId, UUID campaignId, UUID sessionId) {
        Campaign campaign = campaigns.findByIdAndWorldId(campaignId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
        Session session = sessions.findByIdAndCampaignId(sessionId, campaignId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        List<ArcBeat> sessionBeats = beats.findBySessionIdOrderByPositionAscCreatedAtAsc(sessionId);

        Map<UUID, String> arcTitles = arcs.findByCampaignIdOrderByPositionAscCreatedAtAsc(campaignId)
                .stream().collect(Collectors.toMap(Arc::getId, Arc::getTitle));

        // Articles referenced by the session's beats, first-seen order preserved.
        LinkedHashSet<UUID> articleIds = sessionBeats.stream()
                .flatMap(b -> b.getArticleIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<PacketArticle> packetArticles = articleIds.stream()
                .map(id -> articles.findByIdAndWorldId(id, worldId).orElse(null))
                .filter(a -> a != null)
                .map(this::toPacketArticle)
                .toList();

        List<PacketBeat> packetBeats = sessionBeats.stream()
                .map(b -> new PacketBeat(b.getId(), b.getTitle(), b.getBody(), b.isDone(),
                        arcTitles.get(b.getArcId()), List.copyOf(b.getArticleIds())))
                .toList();

        // Statblocks referenced by this session's beats first, then campaign-scoped extras.
        Map<UUID, Statblock> sbById = new LinkedHashMap<>();
        sessionBeats.stream().flatMap(b -> b.getStatblockIds().stream()).forEach(id -> {
            if (!sbById.containsKey(id)) {
                statblocks.findByIdAndWorldId(id, worldId).ifPresent(s -> sbById.put(id, s));
            }
        });
        statblocks.findByWorldIdAndCampaignIdOrderByCreatedAtDesc(worldId, campaignId)
                .forEach(s -> sbById.putIfAbsent(s.getId(), s));
        List<StatblockResponse> packetStatblocks =
                sbById.values().stream().map(StatblockResponse::from).toList();

        return new SessionPacketResponse(SessionResponse.from(session), campaign.getName(),
                packetBeats, packetArticles, packetStatblocks);
    }

    private PacketArticle toPacketArticle(Article a) {
        return new PacketArticle(a.getId(), a.getTitle(), a.getTemplate().name(),
                autoLinker.render(a.getWorldId(), a.getBody()));
    }
}

package com.campaignorganizer.statblock;

import com.campaignorganizer.campaign.Arc;
import com.campaignorganizer.campaign.ArcBeatRepository;
import com.campaignorganizer.campaign.ArcRepository;
import com.campaignorganizer.campaign.CampaignRepository;
import com.campaignorganizer.statblock.StatblockDtos.StatblockRequest;
import com.campaignorganizer.statblock.StatblockDtos.StatblockResponse;
import com.campaignorganizer.wiki.ArticleRepository;
import com.campaignorganizer.world.WorldRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/statblocks")
public class StatblockController {

    private final StatblockRepository statblocks;
    private final ArticleRepository articles;
    private final CampaignRepository campaigns;
    private final WorldRepository worlds;
    private final ArcRepository arcs;
    private final ArcBeatRepository beats;

    public StatblockController(StatblockRepository statblocks, ArticleRepository articles,
                              CampaignRepository campaigns, WorldRepository worlds,
                              ArcRepository arcs, ArcBeatRepository beats) {
        this.statblocks = statblocks;
        this.articles = articles;
        this.campaigns = campaigns;
        this.worlds = worlds;
        this.arcs = arcs;
        this.beats = beats;
    }

    @GetMapping
    public List<StatblockResponse> list(@PathVariable UUID worldId,
                                        @RequestParam(required = false) UUID campaignId) {
        requireWorld(worldId);
        List<Statblock> result = campaignId == null
                ? statblocks.findByWorldIdOrderByCreatedAtDesc(worldId)
                : statblocksForCampaign(worldId, campaignId);
        return result.stream().map(StatblockResponse::from).toList();
    }

    /**
     * Statblocks relevant to a campaign (ADR-0043): those explicitly scoped to it,
     * plus any (even shared) statblock referenced by one of the campaign's beats.
     */
    private List<Statblock> statblocksForCampaign(UUID worldId, UUID campaignId) {
        Map<UUID, Statblock> byId = new LinkedHashMap<>();
        statblocks.findByWorldIdAndCampaignIdOrderByCreatedAtDesc(worldId, campaignId)
                .forEach(s -> byId.put(s.getId(), s));

        List<UUID> arcIds = arcs.findByCampaignIdOrderByPositionAscCreatedAtAsc(campaignId)
                .stream().map(Arc::getId).toList();
        if (!arcIds.isEmpty()) {
            List<UUID> refIds = beats.findLinkedStatblockIdsByArcIds(arcIds);
            if (!refIds.isEmpty()) {
                statblocks.findAllById(refIds).forEach(s -> {
                    if (s.getWorldId().equals(worldId)) {
                        byId.putIfAbsent(s.getId(), s);
                    }
                });
            }
        }
        return new ArrayList<>(byId.values());
    }

    @GetMapping("/{statblockId}")
    public StatblockResponse get(@PathVariable UUID worldId, @PathVariable UUID statblockId) {
        return StatblockResponse.from(findOrThrow(worldId, statblockId));
    }

    @PostMapping
    public ResponseEntity<StatblockResponse> create(@PathVariable UUID worldId,
                                                    @Valid @RequestBody StatblockRequest request) {
        requireWorld(worldId);
        validateLinks(worldId, request);
        Statblock saved = statblocks.save(new Statblock(worldId, request.articleId(),
                request.campaignId(), request.name(), request.stats(), request.notes()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/statblocks/" + saved.getId()))
                .body(StatblockResponse.from(saved));
    }

    @PutMapping("/{statblockId}")
    public StatblockResponse update(@PathVariable UUID worldId, @PathVariable UUID statblockId,
                                    @Valid @RequestBody StatblockRequest request) {
        Statblock statblock = findOrThrow(worldId, statblockId);
        validateLinks(worldId, request);
        statblock.update(request.articleId(), request.campaignId(), request.name(),
                request.stats(), request.notes());
        return StatblockResponse.from(statblocks.save(statblock));
    }

    @DeleteMapping("/{statblockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID statblockId) {
        statblocks.delete(findOrThrow(worldId, statblockId));
    }

    private Statblock findOrThrow(UUID worldId, UUID statblockId) {
        return statblocks.findByIdAndWorldId(statblockId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Statblock not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }

    private void validateLinks(UUID worldId, StatblockRequest request) {
        if (request.articleId() != null && !articles.existsByIdAndWorldId(request.articleId(), worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article not found in this world");
        }
        if (request.campaignId() != null && !campaigns.existsByIdAndWorldId(request.campaignId(), worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Campaign not found in this world");
        }
    }
}

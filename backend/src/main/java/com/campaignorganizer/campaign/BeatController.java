package com.campaignorganizer.campaign;

import com.campaignorganizer.campaign.BeatDtos.BeatRequest;
import com.campaignorganizer.campaign.BeatDtos.BeatResponse;
import com.campaignorganizer.characters.application.statblock.port.published.StatblockQueryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleQueryPort;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
import com.campaignorganizer.campaign.application.session.port.published.SessionQueryPort;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/arcs/{arcId}/beats")
public class BeatController {

    private final ArcBeatRepository beats;
    private final ArcRepository arcs;
    private final CampaignQueryPort campaigns;
    private final SessionQueryPort sessions;
    private final ArticleQueryPort articles;
    private final StatblockQueryPort statblocks;

    public BeatController(ArcBeatRepository beats, ArcRepository arcs, CampaignQueryPort campaigns,
                          SessionQueryPort sessions, ArticleQueryPort articles,
                          StatblockQueryPort statblocks) {
        this.beats = beats;
        this.arcs = arcs;
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.articles = articles;
        this.statblocks = statblocks;
    }

    @GetMapping
    public List<BeatResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                   @PathVariable UUID arcId) {
        requireArc(worldId, campaignId, arcId);
        return beats.findByArcIdOrderByPositionAscCreatedAtAsc(arcId).stream()
                .map(BeatResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<BeatResponse> create(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                               @PathVariable UUID arcId, @Valid @RequestBody BeatRequest request) {
        requireArc(worldId, campaignId, arcId);
        validateLinks(worldId, campaignId, request);
        int position = request.position() == null ? 0 : request.position();
        ArcBeat saved = beats.save(new ArcBeat(arcId, request.articleIdsOrEmpty(),
                request.statblockIdsOrEmpty(), request.sessionId(),
                request.title(), request.body(), request.doneOrDefault(), position));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId
                        + "/arcs/" + arcId + "/beats/" + saved.getId()))
                .body(BeatResponse.from(saved));
    }

    @PutMapping("/{beatId}")
    public BeatResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                               @PathVariable UUID arcId, @PathVariable UUID beatId,
                               @Valid @RequestBody BeatRequest request) {
        requireArc(worldId, campaignId, arcId);
        validateLinks(worldId, campaignId, request);
        ArcBeat beat = beats.findByIdAndArcId(beatId, arcId).orElseThrow(this::beatNotFound);
        int position = request.position() == null ? beat.getPosition() : request.position();
        beat.update(request.articleIdsOrEmpty(), request.statblockIdsOrEmpty(), request.sessionId(),
                request.title(), request.body(), request.doneOrDefault(), position);
        return BeatResponse.from(beats.save(beat));
    }

    @DeleteMapping("/{beatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID arcId, @PathVariable UUID beatId) {
        requireArc(worldId, campaignId, arcId);
        ArcBeat beat = beats.findByIdAndArcId(beatId, arcId).orElseThrow(this::beatNotFound);
        beats.delete(beat);
    }

    private void requireArc(UUID worldId, UUID campaignId, UUID arcId) {
        if (!campaigns.existsInWorld(campaignId, worldId)
                || !arcs.existsByIdAndCampaignId(arcId, campaignId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arc not found");
        }
    }

    private void validateLinks(UUID worldId, UUID campaignId, BeatRequest request) {
        for (UUID articleId : request.articleIdsOrEmpty()) {
            if (!articles.existsInWorld(articleId, worldId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Article not found in this world");
            }
        }
        for (UUID statblockId : request.statblockIdsOrEmpty()) {
            if (!statblocks.existsInWorld(statblockId, worldId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statblock not found in this world");
            }
        }
        if (request.sessionId() != null
                && !sessions.existsInCampaign(request.sessionId(), campaignId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Session not found in this campaign");
        }
    }

    private ResponseStatusException beatNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Beat not found");
    }
}

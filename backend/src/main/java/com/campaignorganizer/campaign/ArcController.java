package com.campaignorganizer.campaign;

import com.campaignorganizer.campaign.ArcDtos.ArcRequest;
import com.campaignorganizer.campaign.ArcDtos.ArcResponse;
import com.campaignorganizer.campaign.application.campaign.port.published.CampaignQueryPort;
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
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/arcs")
public class ArcController {

    private final ArcRepository arcs;
    private final CampaignQueryPort campaigns;

    public ArcController(ArcRepository arcs, CampaignQueryPort campaigns) {
        this.arcs = arcs;
        this.campaigns = campaigns;
    }

    @GetMapping
    public List<ArcResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        requireCampaign(worldId, campaignId);
        return arcs.findByCampaignIdOrderByPositionAscCreatedAtAsc(campaignId).stream()
                .map(ArcResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<ArcResponse> create(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                              @Valid @RequestBody ArcRequest request) {
        requireCampaign(worldId, campaignId);
        ArcStatus status = request.status() == null ? ArcStatus.PLANNED : request.status();
        int position = request.position() == null ? 0 : request.position();
        Arc saved = arcs.save(new Arc(campaignId, request.title(), request.description(), status, position));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId
                        + "/arcs/" + saved.getId()))
                .body(ArcResponse.from(saved));
    }

    @PutMapping("/{arcId}")
    public ArcResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                              @PathVariable UUID arcId, @Valid @RequestBody ArcRequest request) {
        requireCampaign(worldId, campaignId);
        Arc arc = arcs.findByIdAndCampaignId(arcId, campaignId).orElseThrow(this::arcNotFound);
        ArcStatus status = request.status() == null ? arc.getStatus() : request.status();
        int position = request.position() == null ? arc.getPosition() : request.position();
        arc.update(request.title(), request.description(), status, position);
        return ArcResponse.from(arcs.save(arc));
    }

    @DeleteMapping("/{arcId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID arcId) {
        requireCampaign(worldId, campaignId);
        Arc arc = arcs.findByIdAndCampaignId(arcId, campaignId).orElseThrow(this::arcNotFound);
        arcs.delete(arc);
    }

    private void requireCampaign(UUID worldId, UUID campaignId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found");
        }
    }

    private ResponseStatusException arcNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Arc not found");
    }
}

package com.campaignorganizer.campaign;

import com.campaignorganizer.campaign.CampaignDtos.CampaignRequest;
import com.campaignorganizer.campaign.CampaignDtos.CampaignResponse;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
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
@RequestMapping("/api/worlds/{worldId}/campaigns")
public class CampaignController {

    private final CampaignRepository campaigns;
    private final WorldQueryPort worlds;

    public CampaignController(CampaignRepository campaigns, WorldQueryPort worlds) {
        this.campaigns = campaigns;
        this.worlds = worlds;
    }

    @GetMapping
    public List<CampaignResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return campaigns.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(CampaignResponse::from)
                .toList();
    }

    @GetMapping("/{campaignId}")
    public CampaignResponse get(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return CampaignResponse.from(findOrThrow(worldId, campaignId));
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody CampaignRequest request) {
        requireWorld(worldId);
        Campaign saved = campaigns.save(
                new Campaign(worldId, request.name(), request.description(), request.notes()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + saved.getId()))
                .body(CampaignResponse.from(saved));
    }

    @PutMapping("/{campaignId}")
    public CampaignResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                   @Valid @RequestBody CampaignRequest request) {
        Campaign campaign = findOrThrow(worldId, campaignId);
        campaign.update(request.name(), request.description(), request.notes());
        return CampaignResponse.from(campaigns.save(campaign));
    }

    @DeleteMapping("/{campaignId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        campaigns.delete(findOrThrow(worldId, campaignId));
    }

    private Campaign findOrThrow(UUID worldId, UUID campaignId) {
        return campaigns.findByIdAndWorldId(campaignId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Campaign not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }
}

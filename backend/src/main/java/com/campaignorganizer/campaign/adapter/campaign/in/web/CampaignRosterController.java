package com.campaignorganizer.campaign.adapter.campaign.in.web;

import com.campaignorganizer.campaign.adapter.campaign.in.web.CampaignRosterWebDtos.RosterEntryResponse;
import com.campaignorganizer.campaign.adapter.campaign.in.web.CampaignRosterWebDtos.RosterRequest;
import com.campaignorganizer.campaign.application.campaign.port.in.GetCampaignRosterUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.SetCampaignRosterUseCase;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Thin web adapter for a campaign's player roster (ADR-0091). */
@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/roster")
public class CampaignRosterController {

    private final GetCampaignRosterUseCase getUseCase;
    private final SetCampaignRosterUseCase setUseCase;
    private final CampaignRosterWebMapper mapper;

    public CampaignRosterController(GetCampaignRosterUseCase getUseCase,
                                    SetCampaignRosterUseCase setUseCase,
                                    CampaignRosterWebMapper mapper) {
        this.getUseCase = getUseCase;
        this.setUseCase = setUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<RosterEntryResponse> get(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return mapper.toResponses(getUseCase.get(worldId, campaignId));
    }

    @PutMapping
    public List<RosterEntryResponse> put(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                         @Valid @RequestBody RosterRequest request) {
        return mapper.toResponses(
                setUseCase.set(mapper.toSetCommand(worldId, campaignId, request)));
    }
}

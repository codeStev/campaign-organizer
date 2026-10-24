package com.campaignorganizer.interchange.companion.adapter.in.web;

import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.CampaignGlossaryResponse;
import com.campaignorganizer.interchange.companion.adapter.in.web.CompanionWebDtos.CompanionCampaignResponse;
import com.campaignorganizer.interchange.companion.application.port.in.GetCampaignGlossaryUseCase;
import com.campaignorganizer.interchange.companion.application.port.in.ListCompanionCampaignsUseCase;
import com.campaignorganizer.security.CurrentUserPort;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Account-level, campaign-addressed endpoints for companion apps such as DiscordWhispr (ADR-0120). */
@RestController
@RequestMapping("/api/companion")
public class CompanionController {

    private final ListCompanionCampaignsUseCase listCampaigns;
    private final GetCampaignGlossaryUseCase glossary;
    private final CurrentUserPort currentUser;
    private final CompanionWebMapper mapper;

    public CompanionController(ListCompanionCampaignsUseCase listCampaigns, GetCampaignGlossaryUseCase glossary,
                               CurrentUserPort currentUser, CompanionWebMapper mapper) {
        this.listCampaigns = listCampaigns;
        this.glossary = glossary;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @GetMapping("/campaigns")
    public List<CompanionCampaignResponse> campaigns() {
        return listCampaigns.list(currentUser.currentAccountId()).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/campaigns/{campaignId}/glossary")
    public CampaignGlossaryResponse glossary(@PathVariable UUID campaignId) {
        return mapper.toResponse(glossary.get(currentUser.currentAccountId(), campaignId));
    }
}

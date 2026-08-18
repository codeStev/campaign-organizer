package com.campaignorganizer.campaign.adapter.campaign.in.web;

import com.campaignorganizer.campaign.adapter.campaign.in.web.CampaignWebDtos.CampaignRequest;
import com.campaignorganizer.campaign.adapter.campaign.in.web.CampaignWebDtos.CampaignResponse;
import com.campaignorganizer.campaign.application.campaign.port.in.CreateCampaignUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.DeleteCampaignUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.GetCampaignUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.ListCampaignsUseCase;
import com.campaignorganizer.campaign.application.campaign.port.in.UpdateCampaignUseCase;
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

@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns")
public class CampaignController {

    private final CreateCampaignUseCase createUseCase;
    private final UpdateCampaignUseCase updateUseCase;
    private final DeleteCampaignUseCase deleteUseCase;
    private final GetCampaignUseCase getUseCase;
    private final ListCampaignsUseCase listUseCase;
    private final CampaignWebMapper mapper;

    public CampaignController(CreateCampaignUseCase createUseCase, UpdateCampaignUseCase updateUseCase,
                             DeleteCampaignUseCase deleteUseCase, GetCampaignUseCase getUseCase,
                             ListCampaignsUseCase listUseCase, CampaignWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<CampaignResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{campaignId}")
    public CampaignResponse get(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return mapper.toResponse(getUseCase.get(worldId, campaignId));
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody CampaignRequest request) {
        CampaignResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + response.id()))
                .body(response);
    }

    @PutMapping("/{campaignId}")
    public CampaignResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                   @Valid @RequestBody CampaignRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, campaignId, request)));
    }

    @DeleteMapping("/{campaignId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        deleteUseCase.delete(worldId, campaignId);
    }
}

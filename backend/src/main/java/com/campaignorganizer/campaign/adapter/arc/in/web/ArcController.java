package com.campaignorganizer.campaign.adapter.arc.in.web;

import com.campaignorganizer.campaign.adapter.arc.in.web.ArcWebDtos.ArcRequest;
import com.campaignorganizer.campaign.adapter.arc.in.web.ArcWebDtos.ArcResponse;
import com.campaignorganizer.campaign.application.arc.port.in.CreateArcUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.DeleteArcUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.ListArcsUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.UpdateArcUseCase;
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
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/arcs")
public class ArcController {

    private final CreateArcUseCase createUseCase;
    private final UpdateArcUseCase updateUseCase;
    private final DeleteArcUseCase deleteUseCase;
    private final ListArcsUseCase listUseCase;
    private final ArcWebMapper mapper;

    public ArcController(CreateArcUseCase createUseCase, UpdateArcUseCase updateUseCase,
                        DeleteArcUseCase deleteUseCase, ListArcsUseCase listUseCase, ArcWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ArcResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return listUseCase.list(worldId, campaignId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<ArcResponse> create(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                              @Valid @RequestBody ArcRequest request) {
        ArcResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateCommand(worldId, campaignId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId
                        + "/arcs/" + response.id()))
                .body(response);
    }

    @PutMapping("/{arcId}")
    public ArcResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                              @PathVariable UUID arcId, @Valid @RequestBody ArcRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, campaignId, arcId, request)));
    }

    @DeleteMapping("/{arcId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID arcId) {
        deleteUseCase.delete(worldId, campaignId, arcId);
    }
}

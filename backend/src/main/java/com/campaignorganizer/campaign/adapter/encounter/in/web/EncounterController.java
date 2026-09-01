package com.campaignorganizer.campaign.adapter.encounter.in.web;

import com.campaignorganizer.campaign.adapter.encounter.in.web.EncounterWebDtos.EncounterRequest;
import com.campaignorganizer.campaign.adapter.encounter.in.web.EncounterWebDtos.EncounterResponse;
import com.campaignorganizer.campaign.application.encounter.port.in.CreateEncounterUseCase;
import com.campaignorganizer.campaign.application.encounter.port.in.DeleteEncounterUseCase;
import com.campaignorganizer.campaign.application.encounter.port.in.ListEncountersUseCase;
import com.campaignorganizer.campaign.application.encounter.port.in.UpdateEncounterUseCase;
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
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/encounters")
public class EncounterController {

    private final CreateEncounterUseCase createUseCase;
    private final UpdateEncounterUseCase updateUseCase;
    private final DeleteEncounterUseCase deleteUseCase;
    private final ListEncountersUseCase listUseCase;
    private final EncounterWebMapper mapper;

    public EncounterController(CreateEncounterUseCase createUseCase, UpdateEncounterUseCase updateUseCase,
                              DeleteEncounterUseCase deleteUseCase, ListEncountersUseCase listUseCase,
                              EncounterWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<EncounterResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return listUseCase.list(worldId, campaignId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<EncounterResponse> create(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                                    @Valid @RequestBody EncounterRequest request) {
        EncounterResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateCommand(worldId, campaignId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId
                        + "/encounters/" + response.id()))
                .body(response);
    }

    @PutMapping("/{encounterId}")
    public EncounterResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                    @PathVariable UUID encounterId, @Valid @RequestBody EncounterRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, campaignId, encounterId, request)));
    }

    @DeleteMapping("/{encounterId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID encounterId) {
        deleteUseCase.delete(worldId, campaignId, encounterId);
    }
}

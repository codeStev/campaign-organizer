package com.campaignorganizer.campaign.adapter.arc.in.web;

import com.campaignorganizer.campaign.adapter.arc.in.web.BeatWebDtos.BeatRequest;
import com.campaignorganizer.campaign.adapter.arc.in.web.BeatWebDtos.BeatResponse;
import com.campaignorganizer.campaign.application.arc.port.in.CreateBeatUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.DeleteBeatUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.ListBeatsUseCase;
import com.campaignorganizer.campaign.application.arc.port.in.UpdateBeatUseCase;
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
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/arcs/{arcId}/beats")
public class BeatController {

    private final CreateBeatUseCase createUseCase;
    private final UpdateBeatUseCase updateUseCase;
    private final DeleteBeatUseCase deleteUseCase;
    private final ListBeatsUseCase listUseCase;
    private final BeatWebMapper mapper;

    public BeatController(CreateBeatUseCase createUseCase, UpdateBeatUseCase updateUseCase,
                         DeleteBeatUseCase deleteUseCase, ListBeatsUseCase listUseCase,
                         BeatWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<BeatResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                   @PathVariable UUID arcId) {
        return listUseCase.list(worldId, campaignId, arcId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<BeatResponse> create(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                               @PathVariable UUID arcId,
                                               @Valid @RequestBody BeatRequest request) {
        BeatResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateCommand(worldId, campaignId, arcId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId
                        + "/arcs/" + arcId + "/beats/" + response.id()))
                .body(response);
    }

    @PutMapping("/{beatId}")
    public BeatResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                               @PathVariable UUID arcId, @PathVariable UUID beatId,
                               @Valid @RequestBody BeatRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, campaignId, arcId, beatId, request)));
    }

    @DeleteMapping("/{beatId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID arcId, @PathVariable UUID beatId) {
        deleteUseCase.delete(worldId, campaignId, arcId, beatId);
    }
}

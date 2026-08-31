package com.campaignorganizer.campaign.adapter.clock.in.web;

import com.campaignorganizer.campaign.adapter.clock.in.web.ClockWebDtos.ClockRequest;
import com.campaignorganizer.campaign.adapter.clock.in.web.ClockWebDtos.ClockResponse;
import com.campaignorganizer.campaign.application.clock.port.in.CreateClockUseCase;
import com.campaignorganizer.campaign.application.clock.port.in.DeleteClockUseCase;
import com.campaignorganizer.campaign.application.clock.port.in.ListClocksUseCase;
import com.campaignorganizer.campaign.application.clock.port.in.UpdateClockUseCase;
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
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/clocks")
public class ClockController {

    private final CreateClockUseCase createUseCase;
    private final UpdateClockUseCase updateUseCase;
    private final DeleteClockUseCase deleteUseCase;
    private final ListClocksUseCase listUseCase;
    private final ClockWebMapper mapper;

    public ClockController(CreateClockUseCase createUseCase, UpdateClockUseCase updateUseCase,
                          DeleteClockUseCase deleteUseCase, ListClocksUseCase listUseCase,
                          ClockWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<ClockResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return listUseCase.list(worldId, campaignId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<ClockResponse> create(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                                @Valid @RequestBody ClockRequest request) {
        ClockResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateCommand(worldId, campaignId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId
                        + "/clocks/" + response.id()))
                .body(response);
    }

    @PutMapping("/{clockId}")
    public ClockResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                @PathVariable UUID clockId, @Valid @RequestBody ClockRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, campaignId, clockId, request)));
    }

    @DeleteMapping("/{clockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID clockId) {
        deleteUseCase.delete(worldId, campaignId, clockId);
    }
}

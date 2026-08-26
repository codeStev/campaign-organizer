package com.campaignorganizer.handouts.adapter.in.web;

import com.campaignorganizer.handouts.adapter.in.web.HandoutWebDtos.HandoutRequest;
import com.campaignorganizer.handouts.adapter.in.web.HandoutWebDtos.HandoutResponse;
import com.campaignorganizer.handouts.application.port.in.CreateHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.DeleteHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.GetHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.ListHandoutsUseCase;
import com.campaignorganizer.handouts.application.port.in.UpdateHandoutUseCase;
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

/** Thin web adapter for handouts (FR-46). */
@RestController
@RequestMapping("/api/worlds/{worldId}/handouts")
public class HandoutController {

    private final CreateHandoutUseCase createUseCase;
    private final UpdateHandoutUseCase updateUseCase;
    private final DeleteHandoutUseCase deleteUseCase;
    private final ListHandoutsUseCase listUseCase;
    private final GetHandoutUseCase getUseCase;
    private final HandoutWebMapper mapper;

    public HandoutController(CreateHandoutUseCase createUseCase,
                             UpdateHandoutUseCase updateUseCase,
                             DeleteHandoutUseCase deleteUseCase, ListHandoutsUseCase listUseCase,
                             GetHandoutUseCase getUseCase, HandoutWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<HandoutResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{handoutId}")
    public HandoutResponse get(@PathVariable UUID worldId, @PathVariable UUID handoutId) {
        return mapper.toResponse(getUseCase.get(worldId, handoutId));
    }

    @PostMapping
    public ResponseEntity<HandoutResponse> create(@PathVariable UUID worldId,
                                                  @Valid @RequestBody HandoutRequest request) {
        HandoutResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/handouts/" + response.id()))
                .body(response);
    }

    @PutMapping("/{handoutId}")
    public HandoutResponse update(@PathVariable UUID worldId, @PathVariable UUID handoutId,
                                  @Valid @RequestBody HandoutRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, handoutId, request)));
    }

    @DeleteMapping("/{handoutId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID handoutId) {
        deleteUseCase.delete(worldId, handoutId);
    }
}

package com.campaignorganizer.handouts.adapter.in.web;

import com.campaignorganizer.handouts.adapter.in.web.HandoutWebDtos.HandoutRequest;
import com.campaignorganizer.handouts.adapter.in.web.HandoutWebDtos.HandoutResponse;
import com.campaignorganizer.handouts.adapter.in.web.HandoutWebDtos.ReorderHandoutsRequest;
import com.campaignorganizer.handouts.application.port.in.CreateHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.DeleteHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.GetHandoutUseCase;
import com.campaignorganizer.handouts.application.port.in.ListHandoutsUseCase;
import com.campaignorganizer.handouts.application.port.in.ReorderHandoutsUseCase;
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

    /**
     * Constrains {handoutId} to an actual UUID so it doesn't swallow the
     * literal /order route below - without this, Spring can route PUT
     * .../handouts/order to update(handoutId="order") instead, which then
     * fails UUID conversion.
     */
    private static final String UUID_PATTERN =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    private final CreateHandoutUseCase createUseCase;
    private final UpdateHandoutUseCase updateUseCase;
    private final DeleteHandoutUseCase deleteUseCase;
    private final ListHandoutsUseCase listUseCase;
    private final GetHandoutUseCase getUseCase;
    private final ReorderHandoutsUseCase reorderUseCase;
    private final HandoutWebMapper mapper;

    public HandoutController(CreateHandoutUseCase createUseCase,
                             UpdateHandoutUseCase updateUseCase,
                             DeleteHandoutUseCase deleteUseCase, ListHandoutsUseCase listUseCase,
                             GetHandoutUseCase getUseCase, ReorderHandoutsUseCase reorderUseCase,
                             HandoutWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.reorderUseCase = reorderUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<HandoutResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{handoutId:" + UUID_PATTERN + "}")
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

    @PutMapping("/{handoutId:" + UUID_PATTERN + "}")
    public HandoutResponse update(@PathVariable UUID worldId, @PathVariable UUID handoutId,
                                  @Valid @RequestBody HandoutRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, handoutId, request)));
    }

    @PutMapping("/order")
    public List<HandoutResponse> reorder(@PathVariable UUID worldId,
                                         @Valid @RequestBody ReorderHandoutsRequest request) {
        return reorderUseCase.reorder(mapper.toReorderCommand(worldId, request)).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @DeleteMapping("/{handoutId:" + UUID_PATTERN + "}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID handoutId) {
        deleteUseCase.delete(worldId, handoutId);
    }
}

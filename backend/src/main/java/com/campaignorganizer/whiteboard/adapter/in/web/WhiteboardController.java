package com.campaignorganizer.whiteboard.adapter.in.web;

import com.campaignorganizer.whiteboard.adapter.in.web.WhiteboardWebDtos.WhiteboardRequest;
import com.campaignorganizer.whiteboard.adapter.in.web.WhiteboardWebDtos.WhiteboardResponse;
import com.campaignorganizer.whiteboard.application.port.in.CreateWhiteboardUseCase;
import com.campaignorganizer.whiteboard.application.port.in.DeleteWhiteboardUseCase;
import com.campaignorganizer.whiteboard.application.port.in.GetWhiteboardUseCase;
import com.campaignorganizer.whiteboard.application.port.in.ListWhiteboardsUseCase;
import com.campaignorganizer.whiteboard.application.port.in.UpdateWhiteboardUseCase;
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

/** Thin web adapter: maps DTOs ↔ commands/view and delegates to the use cases. */
@RestController
@RequestMapping("/api/worlds/{worldId}/whiteboards")
public class WhiteboardController {

    private final CreateWhiteboardUseCase createUseCase;
    private final UpdateWhiteboardUseCase updateUseCase;
    private final DeleteWhiteboardUseCase deleteUseCase;
    private final GetWhiteboardUseCase getUseCase;
    private final ListWhiteboardsUseCase listUseCase;
    private final WhiteboardWebMapper mapper;

    public WhiteboardController(CreateWhiteboardUseCase createUseCase,
                                UpdateWhiteboardUseCase updateUseCase,
                                DeleteWhiteboardUseCase deleteUseCase,
                                GetWhiteboardUseCase getUseCase,
                                ListWhiteboardsUseCase listUseCase,
                                WhiteboardWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<WhiteboardResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{whiteboardId}")
    public WhiteboardResponse get(@PathVariable UUID worldId, @PathVariable UUID whiteboardId) {
        return mapper.toResponse(getUseCase.get(worldId, whiteboardId));
    }

    @PostMapping
    public ResponseEntity<WhiteboardResponse> create(@PathVariable UUID worldId,
                                                     @Valid @RequestBody WhiteboardRequest request) {
        WhiteboardResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/whiteboards/" + response.id()))
                .body(response);
    }

    @PutMapping("/{whiteboardId}")
    public WhiteboardResponse update(@PathVariable UUID worldId, @PathVariable UUID whiteboardId,
                                     @Valid @RequestBody WhiteboardRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, whiteboardId, request)));
    }

    @DeleteMapping("/{whiteboardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID whiteboardId) {
        deleteUseCase.delete(worldId, whiteboardId);
    }
}

package com.campaignorganizer.characters.adapter.statblock.in.web;

import com.campaignorganizer.characters.adapter.statblock.in.web.StatblockWebDtos.StatblockRequest;
import com.campaignorganizer.characters.adapter.statblock.in.web.StatblockWebDtos.StatblockResponse;
import com.campaignorganizer.characters.application.statblock.port.in.CreateStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.DeleteStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.DuplicateStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.GetStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.ListStatblocksUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.UpdateStatblockUseCase;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/worlds/{worldId}/statblocks")
public class StatblockController {

    private final CreateStatblockUseCase createUseCase;
    private final UpdateStatblockUseCase updateUseCase;
    private final DeleteStatblockUseCase deleteUseCase;
    private final GetStatblockUseCase getUseCase;
    private final ListStatblocksUseCase listUseCase;
    private final DuplicateStatblockUseCase duplicateUseCase;
    private final StatblockWebMapper mapper;

    public StatblockController(CreateStatblockUseCase createUseCase, UpdateStatblockUseCase updateUseCase,
                              DeleteStatblockUseCase deleteUseCase, GetStatblockUseCase getUseCase,
                              ListStatblocksUseCase listUseCase, DuplicateStatblockUseCase duplicateUseCase,
                              StatblockWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.duplicateUseCase = duplicateUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<StatblockResponse> list(@PathVariable UUID worldId,
                                        @RequestParam(required = false) UUID campaignId,
                                        @RequestParam(required = false) String tag) {
        return listUseCase.list(worldId, campaignId, tag).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{statblockId}")
    public StatblockResponse get(@PathVariable UUID worldId, @PathVariable UUID statblockId) {
        return mapper.toResponse(getUseCase.get(worldId, statblockId));
    }

    @PostMapping
    public ResponseEntity<StatblockResponse> create(@PathVariable UUID worldId,
                                                    @Valid @RequestBody StatblockRequest request) {
        StatblockResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/statblocks/" + response.id()))
                .body(response);
    }

    @PutMapping("/{statblockId}")
    public StatblockResponse update(@PathVariable UUID worldId, @PathVariable UUID statblockId,
                                    @Valid @RequestBody StatblockRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, statblockId, request)));
    }

    @DeleteMapping("/{statblockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID statblockId) {
        deleteUseCase.delete(worldId, statblockId);
    }

    @PostMapping("/{statblockId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    public StatblockResponse duplicate(@PathVariable UUID worldId, @PathVariable UUID statblockId) {
        return mapper.toResponse(duplicateUseCase.duplicate(worldId, statblockId));
    }
}

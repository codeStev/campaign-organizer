package com.campaignorganizer.characters.adapter.statblock.in.web;

import com.campaignorganizer.characters.adapter.statblock.in.web.GlobalStatblockWebDtos.GlobalStatblockRequest;
import com.campaignorganizer.characters.adapter.statblock.in.web.GlobalStatblockWebDtos.GlobalStatblockResponse;
import com.campaignorganizer.characters.adapter.statblock.in.web.GlobalStatblockWebDtos.ImportGlobalStatblockRequest;
import com.campaignorganizer.characters.adapter.statblock.in.web.StatblockWebDtos.StatblockResponse;
import com.campaignorganizer.characters.application.statblock.port.in.CreateGlobalStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.DeleteGlobalStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.GetGlobalStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.ImportGlobalStatblockUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.ListGlobalStatblocksUseCase;
import com.campaignorganizer.characters.application.statblock.port.in.UpdateGlobalStatblockUseCase;
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

/** World-independent, system-scoped statblock catalog (ADR-0096). */
@RestController
@RequestMapping("/api/statblocks/global")
public class GlobalStatblockController {

    private final CreateGlobalStatblockUseCase createUseCase;
    private final UpdateGlobalStatblockUseCase updateUseCase;
    private final DeleteGlobalStatblockUseCase deleteUseCase;
    private final GetGlobalStatblockUseCase getUseCase;
    private final ListGlobalStatblocksUseCase listUseCase;
    private final ImportGlobalStatblockUseCase importUseCase;
    private final GlobalStatblockWebMapper mapper;
    private final StatblockWebMapper statblockMapper;

    public GlobalStatblockController(CreateGlobalStatblockUseCase createUseCase,
                                     UpdateGlobalStatblockUseCase updateUseCase,
                                     DeleteGlobalStatblockUseCase deleteUseCase,
                                     GetGlobalStatblockUseCase getUseCase,
                                     ListGlobalStatblocksUseCase listUseCase,
                                     ImportGlobalStatblockUseCase importUseCase,
                                     GlobalStatblockWebMapper mapper, StatblockWebMapper statblockMapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.importUseCase = importUseCase;
        this.mapper = mapper;
        this.statblockMapper = statblockMapper;
    }

    @GetMapping
    public List<GlobalStatblockResponse> list(@RequestParam(required = false) UUID systemId) {
        return listUseCase.list(systemId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{globalStatblockId}")
    public GlobalStatblockResponse get(@PathVariable UUID globalStatblockId) {
        return mapper.toResponse(getUseCase.get(globalStatblockId));
    }

    @PostMapping
    public ResponseEntity<GlobalStatblockResponse> create(
            @Valid @RequestBody GlobalStatblockRequest request) {
        GlobalStatblockResponse response = mapper.toResponse(createUseCase.create(mapper.toCreateCommand(request)));
        return ResponseEntity
                .created(URI.create("/api/statblocks/global/" + response.id()))
                .body(response);
    }

    @PutMapping("/{globalStatblockId}")
    public GlobalStatblockResponse update(@PathVariable UUID globalStatblockId,
                                          @Valid @RequestBody GlobalStatblockRequest request) {
        return mapper.toResponse(updateUseCase.update(mapper.toUpdateCommand(globalStatblockId, request)));
    }

    @DeleteMapping("/{globalStatblockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID globalStatblockId) {
        deleteUseCase.delete(globalStatblockId);
    }

    @PostMapping("/{globalStatblockId}/import")
    @ResponseStatus(HttpStatus.CREATED)
    public StatblockResponse importIntoCampaign(@PathVariable UUID globalStatblockId,
                                                @Valid @RequestBody ImportGlobalStatblockRequest request) {
        return statblockMapper.toResponse(importUseCase.importIntoCampaign(globalStatblockId, request.worldId(),
                request.campaignId(), request.name()));
    }
}

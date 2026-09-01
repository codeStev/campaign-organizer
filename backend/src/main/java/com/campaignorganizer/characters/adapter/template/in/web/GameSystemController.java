package com.campaignorganizer.characters.adapter.template.in.web;

import com.campaignorganizer.characters.adapter.template.in.web.GameSystemWebDtos.GameSystemRequest;
import com.campaignorganizer.characters.adapter.template.in.web.GameSystemWebDtos.GameSystemResponse;
import com.campaignorganizer.characters.application.template.port.in.CreateGameSystemUseCase;
import com.campaignorganizer.characters.application.template.port.in.DeleteGameSystemUseCase;
import com.campaignorganizer.characters.application.template.port.in.GetGameSystemUseCase;
import com.campaignorganizer.characters.application.template.port.in.ListGameSystemsUseCase;
import com.campaignorganizer.characters.application.template.port.in.UpdateGameSystemUseCase;
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

/** World-independent game systems (ADR-0094). */
@RestController
@RequestMapping("/api/game-systems")
public class GameSystemController {

    private final CreateGameSystemUseCase createUseCase;
    private final UpdateGameSystemUseCase updateUseCase;
    private final DeleteGameSystemUseCase deleteUseCase;
    private final GetGameSystemUseCase getUseCase;
    private final ListGameSystemsUseCase listUseCase;
    private final GameSystemWebMapper mapper;

    public GameSystemController(CreateGameSystemUseCase createUseCase, UpdateGameSystemUseCase updateUseCase,
                                DeleteGameSystemUseCase deleteUseCase, GetGameSystemUseCase getUseCase,
                                ListGameSystemsUseCase listUseCase, GameSystemWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<GameSystemResponse> list() {
        return listUseCase.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{systemId}")
    public GameSystemResponse get(@PathVariable UUID systemId) {
        return mapper.toResponse(getUseCase.get(systemId));
    }

    @PostMapping
    public ResponseEntity<GameSystemResponse> create(@Valid @RequestBody GameSystemRequest request) {
        GameSystemResponse response = mapper.toResponse(createUseCase.create(mapper.toCreateCommand(request)));
        return ResponseEntity.created(URI.create("/api/game-systems/" + response.id())).body(response);
    }

    @PutMapping("/{systemId}")
    public GameSystemResponse update(@PathVariable UUID systemId, @Valid @RequestBody GameSystemRequest request) {
        return mapper.toResponse(updateUseCase.update(mapper.toUpdateCommand(systemId, request)));
    }

    @DeleteMapping("/{systemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID systemId) {
        deleteUseCase.delete(systemId);
    }
}

package com.campaignorganizer.campaign.adapter.player.in.web;

import com.campaignorganizer.campaign.adapter.player.in.web.PlayerWebDtos.PlayerRequest;
import com.campaignorganizer.campaign.adapter.player.in.web.PlayerWebDtos.PlayerResponse;
import com.campaignorganizer.campaign.application.player.port.in.CreatePlayerUseCase;
import com.campaignorganizer.campaign.application.player.port.in.DeletePlayerUseCase;
import com.campaignorganizer.campaign.application.player.port.in.GetPlayerUseCase;
import com.campaignorganizer.campaign.application.player.port.in.ListPlayersUseCase;
import com.campaignorganizer.campaign.application.player.port.in.UpdatePlayerUseCase;
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
@RequestMapping("/api/worlds/{worldId}/players")
public class PlayerController {

    private final CreatePlayerUseCase createUseCase;
    private final UpdatePlayerUseCase updateUseCase;
    private final DeletePlayerUseCase deleteUseCase;
    private final GetPlayerUseCase getUseCase;
    private final ListPlayersUseCase listUseCase;
    private final PlayerWebMapper mapper;

    public PlayerController(CreatePlayerUseCase createUseCase, UpdatePlayerUseCase updateUseCase,
                            DeletePlayerUseCase deleteUseCase, GetPlayerUseCase getUseCase,
                            ListPlayersUseCase listUseCase, PlayerWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.getUseCase = getUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<PlayerResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/{playerId}")
    public PlayerResponse get(@PathVariable UUID worldId, @PathVariable UUID playerId) {
        return mapper.toResponse(getUseCase.get(worldId, playerId));
    }

    @PostMapping
    public ResponseEntity<PlayerResponse> create(@PathVariable UUID worldId,
                                                 @Valid @RequestBody PlayerRequest request) {
        PlayerResponse response =
                mapper.toResponse(createUseCase.create(mapper.toCreateCommand(worldId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/players/" + response.id()))
                .body(response);
    }

    @PutMapping("/{playerId}")
    public PlayerResponse update(@PathVariable UUID worldId, @PathVariable UUID playerId,
                                 @Valid @RequestBody PlayerRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, playerId, request)));
    }

    @DeleteMapping("/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID playerId) {
        deleteUseCase.delete(worldId, playerId);
    }
}

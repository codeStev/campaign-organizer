package com.campaignorganizer.campaign.adapter.session.in.web;

import com.campaignorganizer.campaign.adapter.session.in.web.SessionWebDtos.SessionRequest;
import com.campaignorganizer.campaign.adapter.session.in.web.SessionWebDtos.SessionResponse;
import com.campaignorganizer.campaign.application.session.port.in.CreateSessionUseCase;
import com.campaignorganizer.campaign.application.session.port.in.DeleteSessionUseCase;
import com.campaignorganizer.campaign.application.session.port.in.ListSessionsUseCase;
import com.campaignorganizer.campaign.application.session.port.in.UpdateSessionUseCase;
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
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/sessions")
public class SessionController {

    private final CreateSessionUseCase createUseCase;
    private final UpdateSessionUseCase updateUseCase;
    private final DeleteSessionUseCase deleteUseCase;
    private final ListSessionsUseCase listUseCase;
    private final SessionWebMapper mapper;

    public SessionController(CreateSessionUseCase createUseCase, UpdateSessionUseCase updateUseCase,
                            DeleteSessionUseCase deleteUseCase, ListSessionsUseCase listUseCase,
                            SessionWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<SessionResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return listUseCase.list(worldId, campaignId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<SessionResponse> create(@PathVariable UUID worldId,
                                                  @PathVariable UUID campaignId,
                                                  @Valid @RequestBody SessionRequest request) {
        SessionResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateCommand(worldId, campaignId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId
                        + "/sessions/" + response.id()))
                .body(response);
    }

    @PutMapping("/{sessionId}")
    public SessionResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                  @PathVariable UUID sessionId, @Valid @RequestBody SessionRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, campaignId, sessionId, request)));
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID sessionId) {
        deleteUseCase.delete(worldId, campaignId, sessionId);
    }
}

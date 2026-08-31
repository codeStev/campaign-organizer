package com.campaignorganizer.campaign.adapter.loosethread.in.web;

import com.campaignorganizer.campaign.adapter.loosethread.in.web.LooseThreadWebDtos.LooseThreadRequest;
import com.campaignorganizer.campaign.adapter.loosethread.in.web.LooseThreadWebDtos.LooseThreadResponse;
import com.campaignorganizer.campaign.application.loosethread.port.in.CreateLooseThreadUseCase;
import com.campaignorganizer.campaign.application.loosethread.port.in.DeleteLooseThreadUseCase;
import com.campaignorganizer.campaign.application.loosethread.port.in.ListLooseThreadsUseCase;
import com.campaignorganizer.campaign.application.loosethread.port.in.UpdateLooseThreadUseCase;
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
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/loose-threads")
public class LooseThreadController {

    private final CreateLooseThreadUseCase createUseCase;
    private final UpdateLooseThreadUseCase updateUseCase;
    private final DeleteLooseThreadUseCase deleteUseCase;
    private final ListLooseThreadsUseCase listUseCase;
    private final LooseThreadWebMapper mapper;

    public LooseThreadController(CreateLooseThreadUseCase createUseCase,
                                 UpdateLooseThreadUseCase updateUseCase,
                                 DeleteLooseThreadUseCase deleteUseCase,
                                 ListLooseThreadsUseCase listUseCase, LooseThreadWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<LooseThreadResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                          @PathVariable UUID sessionId) {
        return listUseCase.list(worldId, campaignId, sessionId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<LooseThreadResponse> create(@PathVariable UUID worldId,
                                                       @PathVariable UUID campaignId,
                                                       @PathVariable UUID sessionId,
                                                       @Valid @RequestBody LooseThreadRequest request) {
        LooseThreadResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateCommand(worldId, campaignId, sessionId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/sessions/"
                        + sessionId + "/loose-threads/" + response.id()))
                .body(response);
    }

    @PutMapping("/{threadId}")
    public LooseThreadResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                      @PathVariable UUID sessionId, @PathVariable UUID threadId,
                                      @Valid @RequestBody LooseThreadRequest request) {
        return mapper.toResponse(updateUseCase.update(
                mapper.toUpdateCommand(worldId, campaignId, sessionId, threadId, request)));
    }

    @DeleteMapping("/{threadId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                       @PathVariable UUID sessionId, @PathVariable UUID threadId) {
        deleteUseCase.delete(worldId, campaignId, sessionId, threadId);
    }
}

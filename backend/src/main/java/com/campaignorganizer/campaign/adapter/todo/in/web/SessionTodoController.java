package com.campaignorganizer.campaign.adapter.todo.in.web;

import com.campaignorganizer.campaign.adapter.todo.in.web.TodoWebDtos.TodoRequest;
import com.campaignorganizer.campaign.adapter.todo.in.web.TodoWebDtos.TodoResponse;
import com.campaignorganizer.campaign.application.todo.port.in.CreateSessionTodoUseCase;
import com.campaignorganizer.campaign.application.todo.port.in.ListSessionTodosUseCase;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A session's todos (ADR-0092); update/delete for these rows live on {@link CampaignTodoController}. */
@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/sessions/{sessionId}/todos")
public class SessionTodoController {

    private final CreateSessionTodoUseCase createUseCase;
    private final ListSessionTodosUseCase listUseCase;
    private final TodoWebMapper mapper;

    public SessionTodoController(CreateSessionTodoUseCase createUseCase, ListSessionTodosUseCase listUseCase,
                                 TodoWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<TodoResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                   @PathVariable UUID sessionId) {
        return listUseCase.list(worldId, campaignId, sessionId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<TodoResponse> create(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                               @PathVariable UUID sessionId,
                                               @Valid @RequestBody TodoRequest request) {
        TodoResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateSessionCommand(worldId, campaignId, sessionId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/todos/"
                        + response.id()))
                .body(response);
    }
}

package com.campaignorganizer.campaign.adapter.todo.in.web;

import com.campaignorganizer.campaign.adapter.todo.in.web.TodoWebDtos.TodoRequest;
import com.campaignorganizer.campaign.adapter.todo.in.web.TodoWebDtos.TodoResponse;
import com.campaignorganizer.campaign.adapter.todo.in.web.TodoWebDtos.TodoUpdateRequest;
import com.campaignorganizer.campaign.application.todo.port.in.CreateCampaignTodoUseCase;
import com.campaignorganizer.campaign.application.todo.port.in.DeleteTodoUseCase;
import com.campaignorganizer.campaign.application.todo.port.in.ListCampaignTodosUseCase;
import com.campaignorganizer.campaign.application.todo.port.in.UpdateTodoUseCase;
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

/** Standing campaign todos, plus the shared update/delete route for any todo (ADR-0092). */
@RestController
@RequestMapping("/api/worlds/{worldId}/campaigns/{campaignId}/todos")
public class CampaignTodoController {

    private final CreateCampaignTodoUseCase createUseCase;
    private final UpdateTodoUseCase updateUseCase;
    private final DeleteTodoUseCase deleteUseCase;
    private final ListCampaignTodosUseCase listUseCase;
    private final TodoWebMapper mapper;

    public CampaignTodoController(CreateCampaignTodoUseCase createUseCase, UpdateTodoUseCase updateUseCase,
                                  DeleteTodoUseCase deleteUseCase, ListCampaignTodosUseCase listUseCase,
                                  TodoWebMapper mapper) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.deleteUseCase = deleteUseCase;
        this.listUseCase = listUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<TodoResponse> list(@PathVariable UUID worldId, @PathVariable UUID campaignId) {
        return listUseCase.list(worldId, campaignId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<TodoResponse> create(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                                               @Valid @RequestBody TodoRequest request) {
        TodoResponse response = mapper.toResponse(
                createUseCase.create(mapper.toCreateCampaignCommand(worldId, campaignId, request)));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/campaigns/" + campaignId + "/todos/"
                        + response.id()))
                .body(response);
    }

    @PutMapping("/{todoId}")
    public TodoResponse update(@PathVariable UUID worldId, @PathVariable UUID campaignId,
                               @PathVariable UUID todoId, @Valid @RequestBody TodoUpdateRequest request) {
        return mapper.toResponse(
                updateUseCase.update(mapper.toUpdateCommand(worldId, campaignId, todoId, request)));
    }

    @DeleteMapping("/{todoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID campaignId, @PathVariable UUID todoId) {
        deleteUseCase.delete(worldId, campaignId, todoId);
    }
}

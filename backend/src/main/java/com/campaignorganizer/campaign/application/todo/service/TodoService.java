package com.campaignorganizer.campaign.application.todo.service;

import com.campaignorganizer.campaign.application.todo.port.in.CreateCampaignTodoUseCase;
import com.campaignorganizer.campaign.application.todo.port.in.CreateSessionTodoUseCase;
import com.campaignorganizer.campaign.application.todo.port.in.DeleteTodoUseCase;
import com.campaignorganizer.campaign.application.todo.port.in.ListCampaignTodosUseCase;
import com.campaignorganizer.campaign.application.todo.port.in.ListSessionTodosUseCase;
import com.campaignorganizer.campaign.application.todo.port.in.TodoCommands.CreateCampaignTodoCommand;
import com.campaignorganizer.campaign.application.todo.port.in.TodoCommands.CreateSessionTodoCommand;
import com.campaignorganizer.campaign.application.todo.port.in.TodoCommands.UpdateTodoCommand;
import com.campaignorganizer.campaign.application.todo.port.in.UpdateTodoUseCase;
import com.campaignorganizer.campaign.application.todo.port.out.CampaignExistsPort;
import com.campaignorganizer.campaign.application.todo.port.out.SessionExistsPort;
import com.campaignorganizer.campaign.application.todo.port.out.TodoRepositoryPort;
import com.campaignorganizer.campaign.application.todo.port.published.TodoImportPort;
import com.campaignorganizer.campaign.application.todo.port.published.TodoQueryPort;
import com.campaignorganizer.campaign.application.todo.port.published.TodoView;
import com.campaignorganizer.campaign.domain.todo.Todo;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Todo use cases (ADR-0092); also implements the published query/import ports. */
@Service
public class TodoService implements CreateCampaignTodoUseCase, CreateSessionTodoUseCase,
        UpdateTodoUseCase, DeleteTodoUseCase, ListCampaignTodosUseCase, ListSessionTodosUseCase,
        TodoQueryPort, TodoImportPort {

    private final TodoRepositoryPort todos;
    private final CampaignExistsPort campaigns;
    private final SessionExistsPort sessions;
    private final TodoViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public TodoService(TodoRepositoryPort todos, CampaignExistsPort campaigns, SessionExistsPort sessions,
                       TodoViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.todos = todos;
        this.campaigns = campaigns;
        this.sessions = sessions;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TodoView create(CreateCampaignTodoCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        Todo created = Todo.create(ids.newId(), command.campaignId(), null, command.text(), false,
                clock.instant());
        return viewMapper.toView(todos.save(created));
    }

    @Override
    @Transactional
    public TodoView create(CreateSessionTodoCommand command) {
        requireSession(command.worldId(), command.campaignId(), command.sessionId());
        Todo created = Todo.create(ids.newId(), command.campaignId(), command.sessionId(), command.text(),
                false, clock.instant());
        return viewMapper.toView(todos.save(created));
    }

    @Override
    @Transactional
    public TodoView update(UpdateTodoCommand command) {
        requireCampaign(command.worldId(), command.campaignId());
        Todo todo = require(command.todoId(), command.campaignId());
        todo.update(command.text(), command.done(), clock.instant());
        return viewMapper.toView(todos.save(todo));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID campaignId, UUID todoId) {
        requireCampaign(worldId, campaignId);
        todos.delete(require(todoId, campaignId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoView> list(UUID worldId, UUID campaignId) {
        requireCampaign(worldId, campaignId);
        return todos.findStandingByCampaign(campaignId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoView> list(UUID worldId, UUID campaignId, UUID sessionId) {
        requireSession(worldId, campaignId, sessionId);
        return findBySession(sessionId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public TodoView importTodo(TodoView view) {
        Todo todo = Todo.reconstitute(view.id(), view.campaignId(), view.sessionId(), view.text(),
                view.done(), view.createdAt(), view.updatedAt());
        return viewMapper.toView(todos.save(todo));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<TodoView> findByCampaign(UUID campaignId) {
        return todos.findByCampaign(campaignId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TodoView> findBySession(UUID sessionId) {
        return todos.findBySession(sessionId).stream().map(viewMapper::toView).toList();
    }

    private Todo require(UUID todoId, UUID campaignId) {
        return todos.findByIdAndCampaign(todoId, campaignId)
                .orElseThrow(() -> new NotFoundException("Todo not found"));
    }

    private void requireCampaign(UUID worldId, UUID campaignId) {
        if (!campaigns.existsInWorld(campaignId, worldId)) {
            throw new NotFoundException("Campaign not found");
        }
    }

    private void requireSession(UUID worldId, UUID campaignId, UUID sessionId) {
        requireCampaign(worldId, campaignId);
        if (!sessions.existsInCampaign(sessionId, campaignId)) {
            throw new NotFoundException("Session not found");
        }
    }
}

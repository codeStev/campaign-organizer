package com.campaignorganizer.campaign.adapter.todo.out.persistence;

import com.campaignorganizer.campaign.application.todo.port.out.TodoRepositoryPort;
import com.campaignorganizer.campaign.domain.todo.Todo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TodoPersistenceAdapter implements TodoRepositoryPort {

    private final TodoJpaRepository repository;
    private final TodoPersistenceMapper mapper;

    public TodoPersistenceAdapter(TodoJpaRepository repository, TodoPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Todo> findStandingByCampaign(UUID campaignId) {
        return repository.findByCampaignIdAndSessionIdIsNullOrderByCreatedAtAsc(campaignId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Todo> findBySession(UUID sessionId) {
        return repository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Todo> findByCampaign(UUID campaignId) {
        return repository.findByCampaignIdOrderByCreatedAtAsc(campaignId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Todo> findByIdAndCampaign(UUID todoId, UUID campaignId) {
        return repository.findByIdAndCampaignId(todoId, campaignId).map(mapper::toDomain);
    }

    @Override
    public Todo save(Todo todo) {
        return mapper.toDomain(repository.save(mapper.toEntity(todo)));
    }

    @Override
    public void delete(Todo todo) {
        repository.deleteById(todo.getId());
    }
}

package com.campaignorganizer.campaign.adapter.todo.out.persistence;

import com.campaignorganizer.campaign.domain.todo.Todo;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface TodoPersistenceMapper {

    TodoJpaEntity toEntity(Todo todo);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default Todo toDomain(TodoJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Todo.reconstitute(
                entity.getId(),
                entity.getCampaignId(),
                entity.getSessionId(),
                entity.getText(),
                entity.isDone(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}

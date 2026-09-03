package com.campaignorganizer.tables.adapter.rolltable.out.persistence;

import com.campaignorganizer.tables.domain.rolltable.RollTable;
import com.campaignorganizer.tables.domain.rolltable.RollTableEntry;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface RollTablePersistenceMapper {

    RollTableJpaEntity toEntity(RollTable table);

    RollTableEntryJson toJson(RollTableEntry entry);

    RollTableEntry toEntry(RollTableEntryJson json);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default RollTable toDomain(RollTableJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return RollTable.reconstitute(
                entity.getId(),
                entity.getWorldId(),
                entity.getCategoryId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getDiceExpression(),
                entity.getMinResult(),
                entity.getMaxResult(),
                entity.getEntries().stream().map(this::toEntry).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}

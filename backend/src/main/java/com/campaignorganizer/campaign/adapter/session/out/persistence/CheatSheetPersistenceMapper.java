package com.campaignorganizer.campaign.adapter.session.out.persistence;

import com.campaignorganizer.campaign.domain.session.CheatSheet;
import com.campaignorganizer.campaign.domain.session.CheatSheetFragment;
import java.util.UUID;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface CheatSheetPersistenceMapper {

    CheatSheetJpaEntity toEntity(CheatSheet sheet);

    CheatSheetFragmentJson toJson(CheatSheetFragment fragment);

    default CheatSheet toDomain(CheatSheetJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return CheatSheet.reconstitute(
                entity.getId(),
                entity.getSessionId(),
                entity.getFragments().stream().map(this::toFragment).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    default CheatSheetFragment toFragment(CheatSheetFragmentJson json) {
        if (json == null) {
            return null;
        }
        return new CheatSheetFragment(
                json.id() == null ? null : UUID.fromString(json.id()),
                CheatSheetFragment.Type.valueOf(json.type()),
                json.text(),
                uuidOrNull(json.statblockId()),
                uuidOrNull(json.tableId()),
                uuidOrNull(json.entryId()),
                uuidOrNull(json.deckId()),
                uuidOrNull(json.cardId()));
    }

    private static UUID uuidOrNull(String value) {
        return value == null ? null : UUID.fromString(value);
    }
}

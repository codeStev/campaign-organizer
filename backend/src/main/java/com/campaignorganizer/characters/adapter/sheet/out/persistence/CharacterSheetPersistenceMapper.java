package com.campaignorganizer.characters.adapter.sheet.out.persistence;

import com.campaignorganizer.characters.domain.sheet.CharacterSheet;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CharacterSheetPersistenceMapper {

    CharacterSheetJpaEntity toEntity(CharacterSheet sheet);

    default CharacterSheet toDomain(CharacterSheetJpaEntity e) {
        if (e == null) {
            return null;
        }
        return CharacterSheet.reconstitute(e.getId(), e.getWorldId(), e.getTemplateId(), e.getArticleId(),
                e.getCampaignId(), e.getName(), e.getValues(), e.getCreatedAt(), e.getUpdatedAt());
    }
}

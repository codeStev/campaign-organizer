package com.campaignorganizer.characters.adapter.document.out.persistence;

import com.campaignorganizer.characters.domain.document.Document;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DocumentPersistenceMapper {

    DocumentJpaEntity toEntity(Document document);

    default Document toDomain(DocumentJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Document.reconstitute(e.getId(), e.getWorldId(), e.getCategoryId(), e.getTemplateId(),
                e.getCampaignId(), e.getName(), e.getValues(), e.getCreatedAt(), e.getUpdatedAt());
    }
}

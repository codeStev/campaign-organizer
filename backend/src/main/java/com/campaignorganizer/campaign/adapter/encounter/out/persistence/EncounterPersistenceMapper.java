package com.campaignorganizer.campaign.adapter.encounter.out.persistence;

import com.campaignorganizer.campaign.domain.encounter.Encounter;
import com.campaignorganizer.campaign.domain.encounter.EncounterEntry;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface EncounterPersistenceMapper {

    EncounterJpaEntity toEntity(Encounter encounter);

    EncounterEntryEmbeddable toEmbeddable(EncounterEntry entry);

    EncounterEntry toEntry(EncounterEntryEmbeddable embeddable);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default Encounter toDomain(EncounterJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Encounter.reconstitute(
                entity.getId(),
                entity.getCampaignId(),
                entity.getName(),
                entity.getNotes(),
                entity.getEntries().stream().map(this::toEntry).toList(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}

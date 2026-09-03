package com.campaignorganizer.campaign.adapter.beatkind.out.persistence;

import com.campaignorganizer.campaign.domain.beatkind.BeatKind;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BeatKindPersistenceMapper {

    BeatKindJpaEntity toEntity(BeatKind beatKind);

    default BeatKind toDomain(BeatKindJpaEntity e) {
        if (e == null) {
            return null;
        }
        return BeatKind.reconstitute(e.getId(), e.getWorldId(), e.getName(), e.getColor(), e.getCreatedAt(),
                e.getUpdatedAt());
    }
}

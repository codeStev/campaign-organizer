package com.campaignorganizer.campaign.adapter.arc.out.persistence;

import com.campaignorganizer.campaign.domain.arc.ArcBeat;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArcBeatPersistenceMapper {

    ArcBeatJpaEntity toEntity(ArcBeat beat);

    default ArcBeat toDomain(ArcBeatJpaEntity e) {
        if (e == null) {
            return null;
        }
        return ArcBeat.reconstitute(e.getId(), e.getArcId(), e.getArticleIds(), e.getStatblockIds(),
                e.getEncounterIds(), e.getTableIds(), e.getDeckIds(), e.getSessionId(), e.getKindId(),
                e.getTitle(), e.getBody(), e.isDone(), e.getPosition(), e.getCreatedAt(), e.getUpdatedAt());
    }
}

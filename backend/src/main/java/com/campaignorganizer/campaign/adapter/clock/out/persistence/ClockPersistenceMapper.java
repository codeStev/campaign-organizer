package com.campaignorganizer.campaign.adapter.clock.out.persistence;

import com.campaignorganizer.campaign.domain.clock.ClockSegment;
import com.campaignorganizer.campaign.domain.clock.GameClock;
import org.mapstruct.Mapper;

/** Maps the domain aggregate to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface ClockPersistenceMapper {

    ClockJpaEntity toEntity(GameClock clock);

    ClockSegmentJson toJson(ClockSegment segment);

    ClockSegment toSegment(ClockSegmentJson json);

    /** The aggregate is immutable with a static factory, so reconstitute it explicitly. */
    default GameClock toDomain(ClockJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return GameClock.reconstitute(
                entity.getId(),
                entity.getCampaignId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getSegments().stream().map(this::toSegment).toList(),
                entity.getPosition(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}

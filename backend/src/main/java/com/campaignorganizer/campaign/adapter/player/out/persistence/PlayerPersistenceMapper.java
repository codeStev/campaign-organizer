package com.campaignorganizer.campaign.adapter.player.out.persistence;

import com.campaignorganizer.campaign.domain.player.Player;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PlayerPersistenceMapper {

    PlayerJpaEntity toEntity(Player player);

    default Player toDomain(PlayerJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Player.reconstitute(e.getId(), e.getWorldId(), e.getName(), e.getCreatedAt(),
                e.getUpdatedAt());
    }
}

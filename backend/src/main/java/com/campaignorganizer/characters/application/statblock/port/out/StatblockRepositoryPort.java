package com.campaignorganizer.characters.application.statblock.port.out;

import com.campaignorganizer.characters.domain.statblock.Statblock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StatblockRepositoryPort {

    List<Statblock> findByWorld(UUID worldId);

    List<Statblock> findByWorldAndCampaign(UUID worldId, UUID campaignId);

    List<Statblock> findByWorldAndArticle(UUID worldId, UUID articleId);

    List<Statblock> findAllByIds(Collection<UUID> ids);

    Optional<Statblock> findByIdAndWorld(UUID statblockId, UUID worldId);

    Statblock save(Statblock statblock);

    void delete(Statblock statblock);
}

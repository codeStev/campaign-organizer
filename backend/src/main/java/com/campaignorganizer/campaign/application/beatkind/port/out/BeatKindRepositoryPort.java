package com.campaignorganizer.campaign.application.beatkind.port.out;

import com.campaignorganizer.campaign.domain.beatkind.BeatKind;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BeatKindRepositoryPort {

    List<BeatKind> findByWorld(UUID worldId);

    Optional<BeatKind> findByIdAndWorld(UUID beatKindId, UUID worldId);

    Optional<BeatKind> findByNameIgnoreCaseAndWorld(String name, UUID worldId);

    BeatKind save(BeatKind beatKind);

    void delete(BeatKind beatKind);
}

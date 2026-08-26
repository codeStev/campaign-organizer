package com.campaignorganizer.handouts.application.port.out;

import com.campaignorganizer.handouts.domain.Handout;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HandoutRepositoryPort {

    List<Handout> findByWorld(UUID worldId);

    Optional<Handout> findByIdAndWorld(UUID handoutId, UUID worldId);

    Optional<Handout> findById(UUID handoutId);

    Handout save(Handout handout);

    void delete(Handout handout);
}

package com.campaignorganizer.worldbuilding.application.timeline.port.out;

import com.campaignorganizer.worldbuilding.domain.timeline.Timeline;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TimelineRepositoryPort {

    List<Timeline> findByWorld(UUID worldId);

    Optional<Timeline> findByIdAndWorld(UUID timelineId, UUID worldId);

    Optional<Timeline> findById(UUID timelineId);

    Timeline save(Timeline timeline);

    void delete(Timeline timeline);
}

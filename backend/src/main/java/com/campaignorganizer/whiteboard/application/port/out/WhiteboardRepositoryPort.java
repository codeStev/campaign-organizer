package com.campaignorganizer.whiteboard.application.port.out;

import com.campaignorganizer.whiteboard.domain.Whiteboard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound persistence port; speaks the domain aggregate, not JPA entities. */
public interface WhiteboardRepositoryPort {

    List<Whiteboard> findByWorld(UUID worldId);

    Optional<Whiteboard> findByIdAndWorld(UUID whiteboardId, UUID worldId);

    Whiteboard save(Whiteboard whiteboard);

    void delete(Whiteboard whiteboard);
}

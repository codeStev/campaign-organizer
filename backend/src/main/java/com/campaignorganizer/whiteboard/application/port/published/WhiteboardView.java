package com.campaignorganizer.whiteboard.application.port.published;

import com.campaignorganizer.whiteboard.domain.Edge;
import com.campaignorganizer.whiteboard.domain.Node;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Published read model for a whiteboard. */
public record WhiteboardView(
        UUID id,
        UUID worldId,
        String name,
        List<Node> nodes,
        List<Edge> edges,
        Instant createdAt,
        Instant updatedAt) {
}

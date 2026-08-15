package com.campaignorganizer.whiteboard.application.port.in;

import com.campaignorganizer.whiteboard.domain.Edge;
import com.campaignorganizer.whiteboard.domain.Node;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Application-layer read model returned by the whiteboard use cases. */
public record WhiteboardView(
        UUID id,
        UUID worldId,
        String name,
        List<Node> nodes,
        List<Edge> edges,
        Instant createdAt,
        Instant updatedAt) {
}

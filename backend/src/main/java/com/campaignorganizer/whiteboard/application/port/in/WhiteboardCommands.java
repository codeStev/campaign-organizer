package com.campaignorganizer.whiteboard.application.port.in;

import com.campaignorganizer.whiteboard.domain.Edge;
import com.campaignorganizer.whiteboard.domain.Node;
import java.util.List;
import java.util.UUID;

/** Inbound command inputs for the whiteboard use cases (carry domain value objects). */
public final class WhiteboardCommands {

    private WhiteboardCommands() {
    }

    public record CreateWhiteboardCommand(UUID worldId, String name, List<Node> nodes, List<Edge> edges) {
    }

    public record UpdateWhiteboardCommand(UUID worldId, UUID whiteboardId, String name,
                                          List<Node> nodes, List<Edge> edges) {
    }
}

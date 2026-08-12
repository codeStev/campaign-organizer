package com.campaignorganizer.whiteboard;

import com.campaignorganizer.whiteboard.WhiteboardSchema.Edge;
import com.campaignorganizer.whiteboard.WhiteboardSchema.Node;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WhiteboardDtos {

    private WhiteboardDtos() {
    }

    public record WhiteboardRequest(
            @NotBlank @Size(max = 200) String name,
            List<Node> nodes,
            List<Edge> edges) {
    }

    public record WhiteboardResponse(
            UUID id,
            UUID worldId,
            String name,
            List<Node> nodes,
            List<Edge> edges,
            Instant createdAt,
            Instant updatedAt) {

        public static WhiteboardResponse from(Whiteboard w) {
            return new WhiteboardResponse(w.getId(), w.getWorldId(), w.getName(),
                    w.getNodes(), w.getEdges(), w.getCreatedAt(), w.getUpdatedAt());
        }
    }
}

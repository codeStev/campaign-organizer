package com.campaignorganizer.whiteboard.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Web request/response models for whiteboards (the API contract; adapter-owned). */
public final class WhiteboardWebDtos {

    private WhiteboardWebDtos() {
    }

    public record NodeDto(String id, String text, double x, double y, String color) {
    }

    public record EdgeDto(String id, String fromNodeId, String toNodeId, String label) {
    }

    public record WhiteboardRequest(
            @NotBlank @Size(max = 200) String name,
            List<NodeDto> nodes,
            List<EdgeDto> edges) {
    }

    public record WhiteboardResponse(
            UUID id,
            UUID worldId,
            String name,
            List<NodeDto> nodes,
            List<EdgeDto> edges,
            Instant createdAt,
            Instant updatedAt) {
    }
}

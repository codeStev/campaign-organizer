package com.campaignorganizer.whiteboard.domain;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A free-form plotting canvas (aggregate root). Pure domain: no framework, no
 * persistence. Identity and timestamps are supplied by the application layer.
 */
public final class Whiteboard {

    private final UUID id;
    private final UUID worldId;
    private String name;
    private List<Node> nodes;
    private List<Edge> edges;
    private final Instant createdAt;
    private Instant updatedAt;

    private Whiteboard(UUID id, UUID worldId, String name, List<Node> nodes, List<Edge> edges,
                       Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.name = requireName(name);
        this.nodes = copy(nodes);
        this.edges = copy(edges);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Create a brand-new whiteboard; {@code now} and {@code id} come from the app layer. */
    public static Whiteboard create(UUID id, UUID worldId, String name, List<Node> nodes,
                                    List<Edge> edges, Instant now) {
        return new Whiteboard(id, worldId, name, nodes, edges, now, now);
    }

    /** Rebuild an existing whiteboard from storage (no invariants re-run beyond name). */
    public static Whiteboard reconstitute(UUID id, UUID worldId, String name, List<Node> nodes,
                                          List<Edge> edges, Instant createdAt, Instant updatedAt) {
        return new Whiteboard(id, worldId, name, nodes, edges, createdAt, updatedAt);
    }

    /** Replace the board's contents (an edit). */
    public void revise(String name, List<Node> nodes, List<Edge> edges, Instant now) {
        this.name = requireName(name);
        this.nodes = copy(nodes);
        this.edges = copy(edges);
        this.updatedAt = now;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Whiteboard name must not be blank");
        }
        return name;
    }

    private static <T> List<T> copy(List<T> items) {
        return items == null ? List.of() : List.copyOf(items);
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public String getName() {
        return name;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

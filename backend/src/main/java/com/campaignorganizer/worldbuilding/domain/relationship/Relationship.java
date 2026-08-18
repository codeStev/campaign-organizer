package com.campaignorganizer.worldbuilding.domain.relationship;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;

/**
 * A directed or undirected link between two articles (aggregate root). Pure domain:
 * enforces the "no self-relation" invariant; endpoint existence is checked by the
 * application via a port.
 */
public final class Relationship {

    private final UUID id;
    private final UUID worldId;
    private UUID fromArticleId;
    private UUID toArticleId;
    private String label;
    private boolean directed;
    private final Instant createdAt;
    private Instant updatedAt;

    private Relationship(UUID id, UUID worldId, UUID fromArticleId, UUID toArticleId, String label,
                         boolean directed, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(fromArticleId, toArticleId, label, directed);
    }

    public static Relationship create(UUID id, UUID worldId, UUID fromArticleId, UUID toArticleId,
                                      String label, boolean directed, Instant now) {
        return new Relationship(id, worldId, fromArticleId, toArticleId, label, directed, now, now);
    }

    public static Relationship reconstitute(UUID id, UUID worldId, UUID fromArticleId, UUID toArticleId,
                                            String label, boolean directed, Instant createdAt,
                                            Instant updatedAt) {
        return new Relationship(id, worldId, fromArticleId, toArticleId, label, directed,
                createdAt, updatedAt);
    }

    public void update(UUID fromArticleId, UUID toArticleId, String label, boolean directed, Instant now) {
        apply(fromArticleId, toArticleId, label, directed);
        this.updatedAt = now;
    }

    private void apply(UUID fromArticleId, UUID toArticleId, String label, boolean directed) {
        if (fromArticleId == null || toArticleId == null) {
            throw new ValidationException("Both endpoints are required");
        }
        if (fromArticleId.equals(toArticleId)) {
            throw new ValidationException("An article cannot relate to itself");
        }
        this.fromArticleId = fromArticleId;
        this.toArticleId = toArticleId;
        this.label = label;
        this.directed = directed;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public UUID getFromArticleId() {
        return fromArticleId;
    }

    public UUID getToArticleId() {
        return toArticleId;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDirected() {
        return directed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

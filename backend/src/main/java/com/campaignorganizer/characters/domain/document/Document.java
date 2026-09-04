package com.campaignorganizer.characters.domain.document;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A filled-in general-purpose document, linked to a DOCUMENT-kind template and optionally a campaign (aggregate root). */
public final class Document {

    private final UUID id;
    private final UUID worldId;
    private UUID categoryId;
    private UUID templateId;
    private UUID campaignId;
    private String name;
    private Map<String, Object> values;
    private final Instant createdAt;
    private Instant updatedAt;

    private Document(UUID id, UUID worldId, UUID categoryId, UUID templateId, UUID campaignId, String name,
                     Map<String, Object> values, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(templateId, campaignId, name, values);
    }

    public static Document create(UUID id, UUID worldId, UUID categoryId, UUID templateId, UUID campaignId,
                                  String name, Map<String, Object> values, Instant now) {
        return new Document(id, worldId, categoryId, templateId, campaignId, name, values, now, now);
    }

    public static Document reconstitute(UUID id, UUID worldId, UUID categoryId, UUID templateId,
                                        UUID campaignId, String name, Map<String, Object> values,
                                        Instant createdAt, Instant updatedAt) {
        return new Document(id, worldId, categoryId, templateId, campaignId, name, values, createdAt,
                updatedAt);
    }

    public void update(UUID categoryId, UUID templateId, UUID campaignId, String name,
                       Map<String, Object> values, Instant now) {
        apply(templateId, campaignId, name, values);
        this.categoryId = categoryId;
        this.updatedAt = now;
    }

    private void apply(UUID templateId, UUID campaignId, String name, Map<String, Object> values) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Document name must not be blank");
        }
        if (templateId == null) {
            throw new ValidationException("Document requires a template");
        }
        this.templateId = templateId;
        this.campaignId = campaignId;
        this.name = name;
        this.values = values == null ? new HashMap<>() : values;
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public UUID getTemplateId() {
        return templateId;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

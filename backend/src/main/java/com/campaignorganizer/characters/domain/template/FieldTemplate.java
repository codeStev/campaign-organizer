package com.campaignorganizer.characters.domain.template;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A reusable field layout within a world (aggregate root) — drives either a
 * character sheet or a statblock, per {@code kind}. See ADR-0024, ADR-0052.
 */
public final class FieldTemplate {

    private final UUID id;
    private final UUID worldId;
    private UUID categoryId;
    private String name;
    private TemplateKind kind;
    private UUID systemId;
    private List<TemplateSection> sections;
    private final Instant createdAt;
    private Instant updatedAt;

    private FieldTemplate(UUID id, UUID worldId, UUID categoryId, String name, TemplateKind kind,
                          UUID systemId, List<TemplateSection> sections, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.categoryId = categoryId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, kind, systemId, sections);
    }

    public static FieldTemplate create(UUID id, UUID worldId, UUID categoryId, String name, TemplateKind kind,
                                       UUID systemId, List<TemplateSection> sections, Instant now) {
        return new FieldTemplate(id, worldId, categoryId, name, kind, systemId, sections, now, now);
    }

    public static FieldTemplate reconstitute(UUID id, UUID worldId, UUID categoryId, String name,
                                             TemplateKind kind, UUID systemId, List<TemplateSection> sections,
                                             Instant createdAt, Instant updatedAt) {
        return new FieldTemplate(id, worldId, categoryId, name, kind, systemId, sections, createdAt,
                updatedAt);
    }

    public void update(UUID categoryId, String name, UUID systemId, List<TemplateSection> sections,
                       Instant now) {
        apply(name, this.kind, systemId, sections);
        this.categoryId = categoryId;
        this.updatedAt = now;
    }

    private void apply(String name, TemplateKind kind, UUID systemId, List<TemplateSection> sections) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Field template name must not be blank");
        }
        if (kind == null) {
            throw new ValidationException("Field template kind must not be null");
        }
        this.name = name;
        this.kind = kind;
        this.systemId = systemId;
        this.sections = sections == null ? new ArrayList<>() : sections;
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

    public String getName() {
        return name;
    }

    public TemplateKind getKind() {
        return kind;
    }

    public UUID getSystemId() {
        return systemId;
    }

    public List<TemplateSection> getSections() {
        return sections;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

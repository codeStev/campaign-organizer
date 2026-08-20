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
    private String name;
    private TemplateKind kind;
    private String system;
    private List<TemplateSection> sections;
    private final Instant createdAt;
    private Instant updatedAt;

    private FieldTemplate(UUID id, UUID worldId, String name, TemplateKind kind, String system,
                          List<TemplateSection> sections, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, kind, system, sections);
    }

    public static FieldTemplate create(UUID id, UUID worldId, String name, TemplateKind kind, String system,
                                       List<TemplateSection> sections, Instant now) {
        return new FieldTemplate(id, worldId, name, kind, system, sections, now, now);
    }

    public static FieldTemplate reconstitute(UUID id, UUID worldId, String name, TemplateKind kind,
                                             String system, List<TemplateSection> sections, Instant createdAt,
                                             Instant updatedAt) {
        return new FieldTemplate(id, worldId, name, kind, system, sections, createdAt, updatedAt);
    }

    public void update(String name, String system, List<TemplateSection> sections, Instant now) {
        apply(name, this.kind, system, sections);
        this.updatedAt = now;
    }

    private void apply(String name, TemplateKind kind, String system, List<TemplateSection> sections) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Field template name must not be blank");
        }
        if (kind == null) {
            throw new ValidationException("Field template kind must not be null");
        }
        this.name = name;
        this.kind = kind;
        this.system = system;
        this.sections = sections == null ? new ArrayList<>() : sections;
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

    public TemplateKind getKind() {
        return kind;
    }

    public String getSystem() {
        return system;
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

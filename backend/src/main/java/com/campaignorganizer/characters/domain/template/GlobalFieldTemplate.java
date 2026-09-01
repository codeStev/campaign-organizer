package com.campaignorganizer.characters.domain.template;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateKind;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A reusable field layout shared across every world (aggregate root) —
 * drives either a character sheet or a statblock, per {@code kind}, keyed
 * by game system rather than by world. See ADR-0093.
 */
public final class GlobalFieldTemplate {

    private final UUID id;
    private String name;
    private TemplateKind kind;
    private UUID systemId;
    private List<TemplateSection> sections;
    private final Instant createdAt;
    private Instant updatedAt;

    private GlobalFieldTemplate(UUID id, String name, TemplateKind kind, UUID systemId,
                                List<TemplateSection> sections, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, kind, systemId, sections);
    }

    public static GlobalFieldTemplate create(UUID id, String name, TemplateKind kind, UUID systemId,
                                             List<TemplateSection> sections, Instant now) {
        return new GlobalFieldTemplate(id, name, kind, systemId, sections, now, now);
    }

    public static GlobalFieldTemplate reconstitute(UUID id, String name, TemplateKind kind, UUID systemId,
                                                    List<TemplateSection> sections, Instant createdAt,
                                                    Instant updatedAt) {
        return new GlobalFieldTemplate(id, name, kind, systemId, sections, createdAt, updatedAt);
    }

    public void update(String name, UUID systemId, List<TemplateSection> sections, Instant now) {
        apply(name, this.kind, systemId, sections);
        this.updatedAt = now;
    }

    private void apply(String name, TemplateKind kind, UUID systemId, List<TemplateSection> sections) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Global field template name must not be blank");
        }
        if (kind == null) {
            throw new ValidationException("Global field template kind must not be null");
        }
        if (systemId == null) {
            throw new ValidationException("Global field template system must not be null");
        }
        this.name = name;
        this.kind = kind;
        this.systemId = systemId;
        this.sections = sections == null ? new ArrayList<>() : sections;
    }

    public UUID getId() {
        return id;
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

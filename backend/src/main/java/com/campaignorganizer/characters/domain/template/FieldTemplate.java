package com.campaignorganizer.characters.domain.template;

import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A reusable field layout for character sheets within a world (aggregate root). */
public final class FieldTemplate {

    private final UUID id;
    private final UUID worldId;
    private String name;
    private String system;
    private List<TemplateSection> sections;
    private final Instant createdAt;
    private Instant updatedAt;

    private FieldTemplate(UUID id, UUID worldId, String name, String system, List<TemplateSection> sections,
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, system, sections);
    }

    public static FieldTemplate create(UUID id, UUID worldId, String name, String system,
                                       List<TemplateSection> sections, Instant now) {
        return new FieldTemplate(id, worldId, name, system, sections, now, now);
    }

    public static FieldTemplate reconstitute(UUID id, UUID worldId, String name, String system,
                                             List<TemplateSection> sections, Instant createdAt,
                                             Instant updatedAt) {
        return new FieldTemplate(id, worldId, name, system, sections, createdAt, updatedAt);
    }

    public void update(String name, String system, List<TemplateSection> sections, Instant now) {
        apply(name, system, sections);
        this.updatedAt = now;
    }

    private void apply(String name, String system, List<TemplateSection> sections) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Field template name must not be blank");
        }
        this.name = name;
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

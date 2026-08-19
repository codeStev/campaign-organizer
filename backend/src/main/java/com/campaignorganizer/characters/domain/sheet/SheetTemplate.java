package com.campaignorganizer.characters.domain.sheet;

import com.campaignorganizer.characters.domain.sheet.SheetSchema.SheetSection;
import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** A reusable field layout for character sheets within a world (aggregate root). */
public final class SheetTemplate {

    private final UUID id;
    private final UUID worldId;
    private String name;
    private String system;
    private List<SheetSection> sections;
    private final Instant createdAt;
    private Instant updatedAt;

    private SheetTemplate(UUID id, UUID worldId, String name, String system, List<SheetSection> sections,
                          Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.worldId = worldId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        apply(name, system, sections);
    }

    public static SheetTemplate create(UUID id, UUID worldId, String name, String system,
                                       List<SheetSection> sections, Instant now) {
        return new SheetTemplate(id, worldId, name, system, sections, now, now);
    }

    public static SheetTemplate reconstitute(UUID id, UUID worldId, String name, String system,
                                             List<SheetSection> sections, Instant createdAt,
                                             Instant updatedAt) {
        return new SheetTemplate(id, worldId, name, system, sections, createdAt, updatedAt);
    }

    public void update(String name, String system, List<SheetSection> sections, Instant now) {
        apply(name, system, sections);
        this.updatedAt = now;
    }

    private void apply(String name, String system, List<SheetSection> sections) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Sheet template name must not be blank");
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

    public List<SheetSection> getSections() {
        return sections;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

package com.campaignorganizer.characters.adapter.template.out.persistence;

import com.campaignorganizer.characters.domain.template.FieldSchema;
import com.campaignorganizer.characters.domain.template.FieldSchema.TemplateSection;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Persistence model for a field template (maps the {@code field_templates} table). */
@Entity
@Table(name = "field_templates")
public class FieldTemplateJpaEntity {

    @Id
    private UUID id;

    @Column(name = "world_id", nullable = false, updatable = false)
    private UUID worldId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FieldSchema.TemplateKind kind;

    @Column(name = "system_id")
    private UUID systemId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<TemplateSection> sections = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FieldTemplateJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorldId() {
        return worldId;
    }

    public void setWorldId(UUID worldId) {
        this.worldId = worldId;
    }

    public UUID getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(UUID categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public FieldSchema.TemplateKind getKind() {
        return kind;
    }

    public void setKind(FieldSchema.TemplateKind kind) {
        this.kind = kind;
    }

    public UUID getSystemId() {
        return systemId;
    }

    public void setSystemId(UUID systemId) {
        this.systemId = systemId;
    }

    public List<TemplateSection> getSections() {
        return sections;
    }

    public void setSections(List<TemplateSection> sections) {
        this.sections = sections == null ? new ArrayList<>() : sections;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}

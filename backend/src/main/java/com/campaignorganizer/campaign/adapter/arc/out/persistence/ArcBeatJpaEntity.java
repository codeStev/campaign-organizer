package com.campaignorganizer.campaign.adapter.arc.out.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistence model for a story beat (maps the {@code arc_beats} table and its link tables). */
@Entity
@Table(name = "arc_beats")
public class ArcBeatJpaEntity {

    @Id
    private UUID id;

    @Column(name = "arc_id", nullable = false, updatable = false)
    private UUID arcId;

    /** Articles this beat concerns (place, NPC, item…); stored in beat_articles (ADR-0032). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "beat_articles", joinColumns = @JoinColumn(name = "beat_id"))
    @Column(name = "article_id")
    private List<UUID> articleIds = new ArrayList<>();

    /** Statblocks this beat uses (goblin spearman, boss…); stored in beat_statblocks (ADR-0043). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "beat_statblocks", joinColumns = @JoinColumn(name = "beat_id"))
    @Column(name = "statblock_id")
    private List<UUID> statblockIds = new ArrayList<>();

    /** Encounters linked to this beat; stored in beat_encounters (ADR-0097). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "beat_encounters", joinColumns = @JoinColumn(name = "beat_id"))
    @Column(name = "encounter_id")
    private List<UUID> encounterIds = new ArrayList<>();

    /** Roll tables referenced from this beat; stored in beat_roll_tables (FR-40). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "beat_roll_tables", joinColumns = @JoinColumn(name = "beat_id"))
    @Column(name = "table_id")
    private List<UUID> tableIds = new ArrayList<>();

    /** Card decks referenced from this beat; stored in beat_card_decks (FR-40). */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "beat_card_decks", joinColumns = @JoinColumn(name = "beat_id"))
    @Column(name = "deck_id")
    private List<UUID> deckIds = new ArrayList<>();

    @Column(name = "session_id")
    private UUID sessionId;

    /** Optional GM-defined beat kind tag (ADR-0101), informational only. */
    @Column(name = "kind_id")
    private UUID kindId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ArcBeatJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getArcId() {
        return arcId;
    }

    public void setArcId(UUID arcId) {
        this.arcId = arcId;
    }

    public List<UUID> getArticleIds() {
        return articleIds;
    }

    public void setArticleIds(List<UUID> articleIds) {
        this.articleIds = articleIds == null ? new ArrayList<>() : articleIds;
    }

    public List<UUID> getStatblockIds() {
        return statblockIds;
    }

    public void setStatblockIds(List<UUID> statblockIds) {
        this.statblockIds = statblockIds == null ? new ArrayList<>() : statblockIds;
    }

    public List<UUID> getEncounterIds() {
        return encounterIds;
    }

    public void setEncounterIds(List<UUID> encounterIds) {
        this.encounterIds = encounterIds == null ? new ArrayList<>() : encounterIds;
    }

    public List<UUID> getTableIds() {
        return tableIds;
    }

    public void setTableIds(List<UUID> tableIds) {
        this.tableIds = tableIds == null ? new ArrayList<>() : tableIds;
    }

    public List<UUID> getDeckIds() {
        return deckIds;
    }

    public void setDeckIds(List<UUID> deckIds) {
        this.deckIds = deckIds == null ? new ArrayList<>() : deckIds;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public void setSessionId(UUID sessionId) {
        this.sessionId = sessionId;
    }

    public UUID getKindId() {
        return kindId;
    }

    public void setKindId(UUID kindId) {
        this.kindId = kindId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isDone() {
        return done;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
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

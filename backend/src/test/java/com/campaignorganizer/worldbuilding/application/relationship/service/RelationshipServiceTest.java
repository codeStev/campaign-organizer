package com.campaignorganizer.worldbuilding.application.relationship.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.relationship.port.in.RelationshipCommands.CreateRelationshipCommand;
import com.campaignorganizer.worldbuilding.application.relationship.port.out.ArticleExistsPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.out.RelationshipRepositoryPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.relationship.port.published.RelationshipView;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test with mocked out-ports — no Spring, no DB. */
@ExtendWith(MockitoExtension.class)
class RelationshipServiceTest {

    @Mock
    private RelationshipRepositoryPort relationships;
    @Mock
    private ArticleExistsPort articles;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final RelationshipViewMapper viewMapper = new RelationshipViewMapperImpl();

    private RelationshipService service;

    @BeforeEach
    void setUp() {
        service = new RelationshipService(relationships, articles, worlds, viewMapper, ids, clock);
    }

    @Test
    void createValidatesEndpointsAndReturnsView() {
        UUID worldId = UUID.randomUUID();
        UUID from = UUID.randomUUID();
        UUID to = UUID.randomUUID();
        UUID newId = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(articles.existsInWorld(any(), any())).thenReturn(true);
        when(ids.newId()).thenReturn(newId);
        when(relationships.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RelationshipView view = service.create(
                new CreateRelationshipCommand(worldId, from, to, "knows", true));

        assertThat(view.id()).isEqualTo(newId);
        assertThat(view.fromArticleId()).isEqualTo(from);
        assertThat(view.toArticleId()).isEqualTo(to);
    }

    @Test
    void createRejectsForeignArticle() {
        UUID worldId = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(articles.existsInWorld(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new CreateRelationshipCommand(worldId, UUID.randomUUID(), UUID.randomUUID(), null, true)))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsUnknownWorld() {
        when(worlds.exists(any())).thenReturn(false);

        assertThatThrownBy(() -> service.create(
                new CreateRelationshipCommand(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null, true)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listForArticleRejectsMissingArticle() {
        when(articles.existsInWorld(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.listForArticle(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }
}

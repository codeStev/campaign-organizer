package com.campaignorganizer.worldbuilding.application.wiki.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.CreateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.UpdateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRevisionRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryQueryPort;
import com.campaignorganizer.worldbuilding.domain.wiki.Article;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleRevision;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test for articles with mocked ports. */
@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private ArticleRepositoryPort articles;
    @Mock
    private ArticleRevisionRepositoryPort revisions;
    @Mock
    private CategoryQueryPort categories;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final ArticleViewMapper viewMapper = new ArticleViewMapperImpl();

    private ArticleService service;

    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ArticleService(articles, revisions, categories, worlds, viewMapper, ids, clock);
    }

    @Test
    void createRejectsMissingWorld() {
        when(worlds.exists(worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateArticleCommand(
                worldId, null, "Goblin", null, ArticleTemplate.GENERIC, "body")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignCategory() {
        UUID categoryId = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(categories.existsInWorld(categoryId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateArticleCommand(
                worldId, categoryId, "Goblin", null, ArticleTemplate.GENERIC, "body")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createDerivesAndDeduplicatesSlugFromTitle() {
        when(worlds.exists(worldId)).thenReturn(true);
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(articles.existsSlugInWorld(worldId, "goblin")).thenReturn(true);
        when(articles.existsSlugInWorld(worldId, "goblin-2")).thenReturn(false);
        when(articles.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArticleView view = service.create(new CreateArticleCommand(
                worldId, null, "Goblin", null, ArticleTemplate.GENERIC, "body"));

        assertThat(view.slug()).isEqualTo("goblin-2");
    }

    @Test
    void updateSnapshotsRevisionBeforeApplying() {
        UUID articleId = UUID.randomUUID();
        Article existing = Article.create(articleId, worldId, null, "Goblin", "goblin",
                ArticleTemplate.GENERIC, "old", clock.instant());
        when(articles.findByIdAndWorld(articleId, worldId)).thenReturn(java.util.Optional.of(existing));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(articles.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(new UpdateArticleCommand(
                worldId, articleId, null, "Goblin", null, ArticleTemplate.GENERIC, "new"));

        verify(revisions).save(any(ArticleRevision.class));
    }
}

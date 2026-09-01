package com.campaignorganizer.worldbuilding.application.wiki.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.CreateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.UpdateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleListQuery;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRevisionRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleTagLookupPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleSummaryView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.CategoryQueryPort;
import com.campaignorganizer.worldbuilding.domain.wiki.Article;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleRevision;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleTemplate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    private ArticleTagLookupPort tagLookup;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final ArticleViewMapper viewMapper = new ArticleViewMapperImpl();

    private ArticleService service;

    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ArticleService(articles, revisions, categories, worlds, tagLookup, viewMapper,
                ids, clock);
        lenient().when(worlds.exists(worldId)).thenReturn(true);
        lenient().when(tagLookup.articleIdsTaggedContaining(any(), any())).thenReturn(Set.of());
    }

    @Test
    void createRejectsMissingWorld() {
        when(worlds.exists(worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateArticleCommand(
                worldId, null, null, "Goblin", null, ArticleTemplate.GENERIC, "body")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignCategory() {
        UUID categoryId = UUID.randomUUID();
        when(categories.existsInWorld(categoryId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateArticleCommand(
                worldId, categoryId, null, "Goblin", null, ArticleTemplate.GENERIC, "body")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createRejectsForeignParent() {
        UUID parentId = UUID.randomUUID();
        when(articles.existsInWorld(parentId, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateArticleCommand(
                worldId, null, parentId, "Goblin", null, ArticleTemplate.GENERIC, "body")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createDerivesAndDeduplicatesSlugFromTitle() {
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(articles.existsSlugInWorld(worldId, "goblin")).thenReturn(true);
        when(articles.existsSlugInWorld(worldId, "goblin-2")).thenReturn(false);
        when(articles.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArticleView view = service.create(new CreateArticleCommand(
                worldId, null, null, "Goblin", null, ArticleTemplate.GENERIC, "body"));

        assertThat(view.slug()).isEqualTo("goblin-2");
    }

    @Test
    void updateSnapshotsRevisionBeforeApplying() {
        UUID articleId = UUID.randomUUID();
        Article existing = Article.create(articleId, worldId, null, null, "Goblin", "goblin",
                ArticleTemplate.GENERIC, "old", clock.instant());
        when(articles.findByIdAndWorld(articleId, worldId)).thenReturn(Optional.of(existing));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(articles.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(new UpdateArticleCommand(
                worldId, articleId, null, null, "Goblin", null, ArticleTemplate.GENERIC, "new"));

        verify(revisions).save(any(ArticleRevision.class));
    }

    @Test
    void updateRejectsSelfParent() {
        UUID articleId = UUID.randomUUID();
        Article existing = Article.create(articleId, worldId, null, null, "Goblin", "goblin",
                ArticleTemplate.GENERIC, "old", clock.instant());
        when(articles.findByIdAndWorld(articleId, worldId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.update(new UpdateArticleCommand(
                worldId, articleId, null, articleId, "Goblin", null, ArticleTemplate.GENERIC, "new")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateRejectsCycleThroughGrandparent() {
        // A -> B -> C today; attempt to set A's parent to C should be rejected.
        UUID aId = UUID.randomUUID();
        UUID bId = UUID.randomUUID();
        UUID cId = UUID.randomUUID();
        Article a = Article.create(aId, worldId, null, null, "A", "a", ArticleTemplate.GENERIC,
                "body", clock.instant());
        Article b = Article.create(bId, worldId, null, aId, "B", "b", ArticleTemplate.GENERIC,
                "body", clock.instant());
        Article c = Article.create(cId, worldId, null, bId, "C", "c", ArticleTemplate.GENERIC,
                "body", clock.instant());

        when(articles.findByIdAndWorld(aId, worldId)).thenReturn(Optional.of(a));
        when(articles.existsInWorld(cId, worldId)).thenReturn(true);
        when(articles.findByIdAndWorld(cId, worldId)).thenReturn(Optional.of(c));
        when(articles.findByIdAndWorld(bId, worldId)).thenReturn(Optional.of(b));

        assertThatThrownBy(() -> service.update(new UpdateArticleCommand(
                worldId, aId, null, cId, "A", null, ArticleTemplate.GENERIC, "body")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void restorePreservesCurrentParentArticleId() {
        UUID articleId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID revisionId = UUID.randomUUID();
        Article existing = Article.create(articleId, worldId, null, parentId, "New title", "new-title",
                ArticleTemplate.GENERIC, "new body", clock.instant());
        // Snapshot captures content only (no parentArticleId) - restore() must
        // preserve the article's *current* parentage, not anything from the revision.
        ArticleRevision revision = ArticleRevision.snapshot(revisionId, existing, clock.instant());

        when(articles.findByIdAndWorld(articleId, worldId)).thenReturn(Optional.of(existing));
        when(revisions.findByIdAndArticle(revisionId, articleId)).thenReturn(Optional.of(revision));
        when(ids.newId()).thenReturn(UUID.randomUUID());
        when(articles.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ArticleView restored = service.restore(worldId, articleId, revisionId);

        assertThat(restored.parentArticleId()).isEqualTo(parentId);
    }

    @Test
    void listAppendsTagOnlyMatchesAfterTextMatches() {
        UUID textMatchId = UUID.randomUUID();
        UUID tagOnlyId = UUID.randomUUID();
        UUID unrelatedId = UUID.randomUUID();
        Article textMatch = Article.create(textMatchId, worldId, null, null, "The Patron's Ledger",
                "patrons-ledger", ArticleTemplate.GENERIC, "body", clock.instant());
        Article tagOnly = Article.create(tagOnlyId, worldId, null, null, "Corvin", "corvin",
                ArticleTemplate.CHARACTER, "no mention here", clock.instant());
        Article unrelated = Article.create(unrelatedId, worldId, null, null, "Somewhere Else",
                "somewhere-else", ArticleTemplate.LOCATION, "body", clock.instant());

        when(articles.search(worldId, "patron")).thenReturn(List.of(textMatch));
        when(tagLookup.articleIdsTaggedContaining(worldId, "patron"))
                .thenReturn(Set.of(tagOnlyId, textMatchId));
        when(articles.findByWorld(worldId)).thenReturn(List.of(unrelated, tagOnly));

        List<ArticleSummaryView> result = service.list(new ArticleListQuery(worldId, null, "patron", null));

        assertThat(result).extracting(ArticleSummaryView::id)
                .containsExactly(textMatchId, tagOnlyId);
    }
}

package com.campaignorganizer.worldbuilding.domain.wiki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Pure domain unit test for the article aggregate. */
class ArticleTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-02-02T00:00:00Z");

    @Test
    void updateBumpsUpdatedAt() {
        Article a = Article.create(UUID.randomUUID(), UUID.randomUUID(), null, null, "Goblin",
                "goblin", ArticleTemplate.CHARACTER, "body", T0);
        a.update(null, null, "Hobgoblin", "hobgoblin", ArticleTemplate.CHARACTER, "body2", T1);

        assertThat(a.getTitle()).isEqualTo("Hobgoblin");
        assertThat(a.getSlug()).isEqualTo("hobgoblin");
        assertThat(a.getCreatedAt()).isEqualTo(T0);
        assertThat(a.getUpdatedAt()).isEqualTo(T1);
    }

    @Test
    void nullTemplateDefaultsToGeneric() {
        Article a = Article.create(UUID.randomUUID(), UUID.randomUUID(), null, null, "Goblin",
                "goblin", null, "body", T0);
        assertThat(a.getTemplate()).isEqualTo(ArticleTemplate.GENERIC);
    }

    @Test
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> Article.create(UUID.randomUUID(), UUID.randomUUID(), null, null,
                " ", "goblin", ArticleTemplate.GENERIC, "body", T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsBlankSlug() {
        assertThatThrownBy(() -> Article.create(UUID.randomUUID(), UUID.randomUUID(), null, null,
                "Goblin", " ", ArticleTemplate.GENERIC, "body", T0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createAndUpdateRoundTripParentArticleId() {
        UUID parentId = UUID.randomUUID();
        Article a = Article.create(UUID.randomUUID(), UUID.randomUUID(), null, parentId, "Room",
                "room", ArticleTemplate.LOCATION, "body", T0);
        assertThat(a.getParentArticleId()).isEqualTo(parentId);

        UUID otherParentId = UUID.randomUUID();
        a.update(null, otherParentId, "Room", "room", ArticleTemplate.LOCATION, "body", T1);
        assertThat(a.getParentArticleId()).isEqualTo(otherParentId);

        a.update(null, null, "Room", "room", ArticleTemplate.LOCATION, "body", T1);
        assertThat(a.getParentArticleId()).isNull();
    }
}

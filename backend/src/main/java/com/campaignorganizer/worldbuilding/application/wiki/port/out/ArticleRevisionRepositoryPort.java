package com.campaignorganizer.worldbuilding.application.wiki.port.out;

import com.campaignorganizer.worldbuilding.domain.wiki.ArticleRevision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRevisionRepositoryPort {

    List<ArticleRevision> findByArticleOrderByCreatedAtDesc(UUID articleId);

    Optional<ArticleRevision> findByIdAndArticle(UUID revisionId, UUID articleId);

    ArticleRevision save(ArticleRevision revision);
}

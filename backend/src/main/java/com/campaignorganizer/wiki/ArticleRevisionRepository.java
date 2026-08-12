package com.campaignorganizer.wiki;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRevisionRepository extends JpaRepository<ArticleRevision, UUID> {

    List<ArticleRevision> findByArticleIdOrderByCreatedAtDesc(UUID articleId);

    Optional<ArticleRevision> findByIdAndArticleId(UUID id, UUID articleId);
}

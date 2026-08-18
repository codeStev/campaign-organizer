package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRevisionJpaRepository extends JpaRepository<ArticleRevisionJpaEntity, UUID> {

    List<ArticleRevisionJpaEntity> findByArticleIdOrderByCreatedAtDesc(UUID articleId);

    Optional<ArticleRevisionJpaEntity> findByIdAndArticleId(UUID id, UUID articleId);
}

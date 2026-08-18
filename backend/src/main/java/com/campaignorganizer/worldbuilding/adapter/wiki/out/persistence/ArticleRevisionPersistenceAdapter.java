package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRevisionRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleRevision;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ArticleRevisionPersistenceAdapter implements ArticleRevisionRepositoryPort {

    private final ArticleRevisionJpaRepository repository;
    private final ArticleRevisionPersistenceMapper mapper;

    public ArticleRevisionPersistenceAdapter(ArticleRevisionJpaRepository repository,
                                             ArticleRevisionPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<ArticleRevision> findByArticleOrderByCreatedAtDesc(UUID articleId) {
        return repository.findByArticleIdOrderByCreatedAtDesc(articleId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public Optional<ArticleRevision> findByIdAndArticle(UUID revisionId, UUID articleId) {
        return repository.findByIdAndArticleId(revisionId, articleId).map(mapper::toDomain);
    }

    @Override
    public ArticleRevision save(ArticleRevision revision) {
        return mapper.toDomain(repository.save(mapper.toEntity(revision)));
    }
}

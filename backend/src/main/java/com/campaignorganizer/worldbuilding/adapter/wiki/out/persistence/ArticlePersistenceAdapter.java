package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleRepositoryPort;
import com.campaignorganizer.worldbuilding.domain.wiki.Article;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ArticlePersistenceAdapter implements ArticleRepositoryPort {

    private final ArticleJpaRepository repository;
    private final ArticlePersistenceMapper mapper;

    public ArticlePersistenceAdapter(ArticleJpaRepository repository, ArticlePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Article> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByCreatedAtDesc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Article> findByWorldAndCategory(UUID worldId, UUID categoryId) {
        return repository.findByWorldIdAndCategoryIdOrderByCreatedAtDesc(worldId, categoryId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public List<Article> search(UUID worldId, String query) {
        return repository.search(worldId, query).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Article> findByIdAndWorld(UUID articleId, UUID worldId) {
        return repository.findByIdAndWorldId(articleId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<Article> findById(UUID articleId) {
        return repository.findById(articleId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID articleId, UUID worldId) {
        return repository.existsByIdAndWorldId(articleId, worldId);
    }

    @Override
    public boolean existsSlugInWorld(UUID worldId, String slug) {
        return repository.existsByWorldIdAndSlug(worldId, slug);
    }

    @Override
    public List<ArticleRef> findRefsByWorld(UUID worldId) {
        return repository.findRefsByWorldId(worldId).stream()
                .map(p -> new ArticleRef(p.getId(), p.getSlug(), p.getTitle()))
                .toList();
    }

    @Override
    public Article save(Article article) {
        return mapper.toDomain(repository.save(mapper.toEntity(article)));
    }

    @Override
    public void delete(Article article) {
        repository.deleteById(article.getId());
    }
}

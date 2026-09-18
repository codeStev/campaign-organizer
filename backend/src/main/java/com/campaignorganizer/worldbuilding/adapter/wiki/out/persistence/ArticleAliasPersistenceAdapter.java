package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import com.campaignorganizer.worldbuilding.application.wiki.port.out.ArticleAliasRepositoryPort;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class ArticleAliasPersistenceAdapter implements ArticleAliasRepositoryPort {

    private final ArticleAliasJpaRepository repository;

    public ArticleAliasPersistenceAdapter(ArticleAliasJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<String> findByArticle(UUID worldId, UUID articleId) {
        return repository.findByWorldIdAndArticleId(worldId, articleId).stream()
                .map(ArticleAliasJpaEntity::getAlias)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    @Override
    public Map<UUID, List<String>> findAllByWorld(UUID worldId) {
        return repository.findByWorldId(worldId).stream()
                .collect(Collectors.groupingBy(ArticleAliasJpaEntity::getArticleId,
                        Collectors.mapping(ArticleAliasJpaEntity::getAlias, Collectors.toList())));
    }

    @Override
    public void replaceAll(UUID worldId, UUID articleId, List<String> aliases) {
        repository.deleteByWorldIdAndArticleId(worldId, articleId);
        for (String alias : aliases) {
            repository.save(new ArticleAliasJpaEntity(articleId, worldId, alias));
        }
    }
}

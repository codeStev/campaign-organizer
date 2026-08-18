package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import com.campaignorganizer.worldbuilding.domain.wiki.Article;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticlePersistenceMapper {

    ArticleJpaEntity toEntity(Article article);

    default Article toDomain(ArticleJpaEntity e) {
        if (e == null) {
            return null;
        }
        return Article.reconstitute(e.getId(), e.getWorldId(), e.getCategoryId(), e.getTitle(),
                e.getSlug(), e.getTemplate(), e.getBody(), e.getCreatedAt(), e.getUpdatedAt());
    }
}

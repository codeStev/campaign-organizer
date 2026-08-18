package com.campaignorganizer.worldbuilding.adapter.wiki.out.persistence;

import com.campaignorganizer.worldbuilding.domain.wiki.ArticleRevision;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleRevisionPersistenceMapper {

    ArticleRevisionJpaEntity toEntity(ArticleRevision revision);

    default ArticleRevision toDomain(ArticleRevisionJpaEntity e) {
        if (e == null) {
            return null;
        }
        return ArticleRevision.reconstitute(e.getId(), e.getArticleId(), e.getTitle(), e.getSlug(),
                e.getTemplate(), e.getBody(), e.getCreatedAt());
    }
}

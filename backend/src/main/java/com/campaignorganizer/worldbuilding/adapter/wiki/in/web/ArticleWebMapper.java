package com.campaignorganizer.worldbuilding.adapter.wiki.in.web;

import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.ArticleRequest;
import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.ArticleResponse;
import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.ArticleSummaryResponse;
import com.campaignorganizer.worldbuilding.adapter.wiki.in.web.ArticleWebDtos.RevisionResponse;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.CreateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.UpdateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRevisionView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleSummaryView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ArticleWebMapper {

    @Mapping(target = "bodyHtml", source = "bodyHtml")
    ArticleResponse toResponse(ArticleView view, String bodyHtml);

    ArticleSummaryResponse toSummary(ArticleSummaryView view);

    RevisionResponse toRevisionResponse(ArticleRevisionView view);

    default CreateArticleCommand toCreateCommand(UUID worldId, ArticleRequest request) {
        return new CreateArticleCommand(worldId, request.categoryId(), request.parentArticleId(),
                request.title(), request.slug(), request.template(), request.body());
    }

    default UpdateArticleCommand toUpdateCommand(UUID worldId, UUID articleId, ArticleRequest request) {
        return new UpdateArticleCommand(worldId, articleId, request.categoryId(),
                request.parentArticleId(), request.title(), request.slug(), request.template(),
                request.body());
    }
}

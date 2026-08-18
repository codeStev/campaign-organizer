package com.campaignorganizer.worldbuilding.application.wiki.service;

import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleRevisionView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleSummaryView;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;
import com.campaignorganizer.worldbuilding.domain.wiki.Article;
import com.campaignorganizer.worldbuilding.domain.wiki.ArticleRevision;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ArticleViewMapper {

    ArticleView toView(Article article);

    ArticleSummaryView toSummary(Article article);

    ArticleRevisionView toRevisionView(ArticleRevision revision);
}

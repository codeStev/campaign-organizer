package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleSummaryView;
import java.util.List;

public interface ListArticlesUseCase {

    List<ArticleSummaryView> list(ArticleListQuery query);
}

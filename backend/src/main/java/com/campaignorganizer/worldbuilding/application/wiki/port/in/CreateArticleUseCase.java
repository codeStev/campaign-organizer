package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.CreateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;

public interface CreateArticleUseCase {

    ArticleView create(CreateArticleCommand command);
}

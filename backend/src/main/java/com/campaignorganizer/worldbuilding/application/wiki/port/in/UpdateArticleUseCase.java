package com.campaignorganizer.worldbuilding.application.wiki.port.in;

import com.campaignorganizer.worldbuilding.application.wiki.port.in.ArticleCommands.UpdateArticleCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.published.ArticleView;

public interface UpdateArticleUseCase {

    ArticleView update(UpdateArticleCommand command);
}

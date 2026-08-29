package com.campaignorganizer.ai.application.port.in;

import com.campaignorganizer.ai.domain.ArticleKind;
import com.campaignorganizer.ai.domain.DraftLevel;
import com.campaignorganizer.ai.domain.DraftResult;

public interface DraftArticleTextUseCase {

    DraftResult draft(DraftArticleTextCommand command);

    record DraftArticleTextCommand(
            String instructions, String existingContent, DraftLevel level, ArticleKind kind) {
    }
}

package com.campaignorganizer.ai.application.port.in;

import com.campaignorganizer.ai.domain.DraftResult;

public interface DraftArticleTextUseCase {

    DraftResult draft(DraftArticleTextCommand command);

    record DraftArticleTextCommand(String instructions, String existingContent) {
    }
}

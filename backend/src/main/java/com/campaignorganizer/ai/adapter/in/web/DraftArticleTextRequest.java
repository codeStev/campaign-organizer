package com.campaignorganizer.ai.adapter.in.web;

import com.campaignorganizer.ai.domain.ArticleKind;
import com.campaignorganizer.ai.domain.DraftLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DraftArticleTextRequest(
        @NotBlank @Size(max = 2000) String instructions,
        @Size(max = 20000) String existingContent,
        DraftLevel level,
        ArticleKind template) {
}

package com.campaignorganizer.ai.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DraftArticleTextRequest(
        @NotBlank @Size(max = 2000) String instructions,
        @Size(max = 20000) String existingContent) {
}

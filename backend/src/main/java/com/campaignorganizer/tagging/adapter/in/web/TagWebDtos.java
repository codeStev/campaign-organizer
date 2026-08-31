package com.campaignorganizer.tagging.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Web request/response models for tags (ADR-0083). */
public final class TagWebDtos {

    private TagWebDtos() {
    }

    public record EntityTagsRequest(
            @NotNull List<@NotBlank @Size(max = 100) String> tags) {
    }

    public record EntityTagsResponse(List<String> tags) {
    }
}

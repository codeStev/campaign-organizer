package com.campaignorganizer.ai.adapter.in.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AiSettingsRequest(@NotEmpty @Valid List<ProviderInput> providers) {

    /** List order becomes priority order. */
    public record ProviderInput(@NotBlank String providerId, String model) {
    }
}

package com.campaignorganizer.ai.domain;

import com.campaignorganizer.shared.domain.ValidationException;

/** A successful draft, tagged with which provider produced it. See ADR-0064. */
public record DraftResult(String text, String provider) {

    public DraftResult {
        if (text == null || text.isBlank()) {
            throw new ValidationException("Generated text was empty");
        }
        if (provider == null || provider.isBlank()) {
            throw new ValidationException("Provider must be set");
        }
    }
}

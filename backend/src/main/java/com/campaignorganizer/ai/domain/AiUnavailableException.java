package com.campaignorganizer.ai.domain;

import com.campaignorganizer.shared.domain.DomainException;

/** Every configured text-generation provider failed (or none is configured). */
public final class AiUnavailableException extends DomainException {

    public AiUnavailableException(String message) {
        super(message);
    }
}

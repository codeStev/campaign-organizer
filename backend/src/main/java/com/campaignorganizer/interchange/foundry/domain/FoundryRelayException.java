package com.campaignorganizer.interchange.foundry.domain;

import com.campaignorganizer.shared.domain.DomainException;

/**
 * The Foundry relay couldn't be reached, rejected the request, or the target
 * Foundry session isn't connected (ADR-0115). Mirrors {@code
 * ai.domain.AiUnavailableException}'s role: a domain exception representing
 * "the external side of this integration is unavailable right now," mapped
 * centrally to {@code 503 Service Unavailable} by {@code DomainExceptionAdvice}.
 */
public final class FoundryRelayException extends DomainException {

    public FoundryRelayException(String message) {
        super(message);
    }
}

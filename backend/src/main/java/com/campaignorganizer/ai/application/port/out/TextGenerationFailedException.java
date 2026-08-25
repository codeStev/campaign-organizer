package com.campaignorganizer.ai.application.port.out;

/**
 * Signals that one {@link TextGenerationPort} implementation couldn't fulfill a
 * request. Internal control flow between an adapter and
 * {@code DraftArticleTextService} only — never thrown past the application layer;
 * the service converts an all-providers-failed situation into the domain-level
 * {@code AiUnavailableException} instead.
 */
public final class TextGenerationFailedException extends RuntimeException {

    public TextGenerationFailedException(String message) {
        super(message);
    }

    public TextGenerationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}

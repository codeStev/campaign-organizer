package com.campaignorganizer.shared.domain;

/**
 * Base type for business-rule failures. Thrown by domain/application code; mapped to
 * an HTTP {@code application/problem+json} response by the web layer (ADR-0009).
 * Carries no HTTP/web concern — see {@code shared.web.DomainExceptionAdvice}.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }
}

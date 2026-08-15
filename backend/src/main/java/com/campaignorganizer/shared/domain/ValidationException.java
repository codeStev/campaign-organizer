package com.campaignorganizer.shared.domain;

/** A domain invariant or input rule was violated. Mapped to HTTP 400. */
public class ValidationException extends DomainException {

    public ValidationException(String message) {
        super(message);
    }
}

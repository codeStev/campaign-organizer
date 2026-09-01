package com.campaignorganizer.shared.domain;

/** An operation conflicts with existing state (e.g. still referenced elsewhere). Mapped to HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}

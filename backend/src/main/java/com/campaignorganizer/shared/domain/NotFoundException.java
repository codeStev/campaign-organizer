package com.campaignorganizer.shared.domain;

/** A referenced aggregate does not exist. Mapped to HTTP 404. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}

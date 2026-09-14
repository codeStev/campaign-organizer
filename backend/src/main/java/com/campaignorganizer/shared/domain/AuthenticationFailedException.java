package com.campaignorganizer.shared.domain;

/**
 * A presented credential (TOTP code, recovery code, ...) was rejected. Mapped to HTTP 401 —
 * distinct from {@link ValidationException} (400, malformed input) and {@link NotFoundException}
 * (404, no such resource): the request was well-formed and its target exists, but the proof of
 * identity it carried was wrong.
 */
public class AuthenticationFailedException extends DomainException {

    public AuthenticationFailedException(String message) {
        super(message);
    }
}

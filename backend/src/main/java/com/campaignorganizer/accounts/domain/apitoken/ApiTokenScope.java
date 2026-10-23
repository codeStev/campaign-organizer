package com.campaignorganizer.accounts.domain.apitoken;

import com.campaignorganizer.shared.domain.ValidationException;
import java.util.Arrays;

/** What an API token may do (ADR-0119). A token only works on endpoints that name one of its scopes. */
public enum ApiTokenScope {
    CAMPAIGNS_READ("campaigns:read"),
    GLOSSARY_READ("glossary:read"),
    SESSIONS_WRITE("sessions:write"),
    TRANSCRIPTS_WRITE("transcripts:write");

    private final String wireValue;

    ApiTokenScope(String wireValue) {
        this.wireValue = wireValue;
    }

    public String wireValue() {
        return wireValue;
    }

    public static ApiTokenScope fromWireValue(String value) {
        return Arrays.stream(values())
                .filter(scope -> scope.wireValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Unknown API token scope: " + value));
    }
}

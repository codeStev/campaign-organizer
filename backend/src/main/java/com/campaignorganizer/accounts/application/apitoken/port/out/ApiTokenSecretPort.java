package com.campaignorganizer.accounts.application.apitoken.port.out;

/** Generates and hashes API token secrets, so the application layer needs no crypto or randomness (ADR-0119). */
public interface ApiTokenSecretPort {

    GeneratedSecret generate();

    String hash(String secret);

    /** {@code secret} is shown once; {@code hash} and {@code prefix} are what gets stored. */
    record GeneratedSecret(String secret, String hash, String prefix) {
    }
}

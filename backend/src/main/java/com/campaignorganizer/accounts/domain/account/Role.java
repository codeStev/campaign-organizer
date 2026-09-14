package com.campaignorganizer.accounts.domain.account;

/**
 * ADMIN additionally grants account-roster management (ADR-0109); it never
 * grants access to another account's worlds or content.
 */
public enum Role {
    ADMIN,
    USER
}

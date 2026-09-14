package com.campaignorganizer.accounts.application.account.port.in;

import com.campaignorganizer.accounts.application.account.port.in.AccountCommands.RegisterAccountCommand;

/**
 * Deliberately returns nothing distinguishing a fresh registration from a
 * duplicate email (ADR-0110, anti-enumeration) — both branches behave
 * identically from the caller's perspective.
 */
public interface RegisterAccountUseCase {

    void register(RegisterAccountCommand command);
}

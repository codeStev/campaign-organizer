package com.campaignorganizer.accounts.application.account.port.in;

public final class AccountCommands {

    private AccountCommands() {
    }

    public record RegisterAccountCommand(String email, String rawPassword) {
    }
}

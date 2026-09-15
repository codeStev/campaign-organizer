package com.campaignorganizer.accounts.application.mfa.port.in;

import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialSummary;
import java.util.List;
import java.util.UUID;

/** Self-service listing of the current account's registered WebAuthn credentials (ADR-0111 follow-up). */
public interface ListWebauthnCredentialsUseCase {

    List<WebAuthnCredentialSummary> listWebauthnCredentials(UUID accountId);
}

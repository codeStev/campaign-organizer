package com.campaignorganizer.accounts.application.mfa.service;

import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.mfa.port.in.ListWebauthnCredentialsUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.RemoveWebauthnCredentialUseCase;
import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialRepositoryPort;
import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialSummary;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Self-service listing/removal of the current account's own WebAuthn credentials (ADR-0111
 * follow-up) — kept separate from {@code MfaService} the same way that class is already split
 * out from {@code AccountService}, since this is a distinct settings-page concern from
 * enrollment/challenge/recovery. Adding a credential needs no use case here at all: the
 * ceremony itself (Spring's own {@code POST /webauthn/register}, gated by
 * {@code WebAuthnCredentialRepositoryAdapter.save()}) is the whole "add" flow.
 */
@Service
public class WebauthnCredentialService implements ListWebauthnCredentialsUseCase, RemoveWebauthnCredentialUseCase {

    private final WebAuthnCredentialRepositoryPort credentials;
    private final AccountQueryPort accounts;

    public WebauthnCredentialService(WebAuthnCredentialRepositoryPort credentials, AccountQueryPort accounts) {
        this.credentials = credentials;
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebAuthnCredentialSummary> listWebauthnCredentials(UUID accountId) {
        return credentials.findByAccountId(accountId);
    }

    @Override
    @Transactional
    public void removeWebauthnCredential(UUID accountId, UUID credentialId) {
        AccountView account = accounts.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Account not found"));
        if (account.mfaMethod() == MfaMethod.WEBAUTHN && credentials.findByAccountId(accountId).size() <= 1) {
            throw new ValidationException(
                    "Cannot remove your only passkey while WebAuthn is your active MFA method — "
                            + "add a backup passkey first, or ask an admin to reset your MFA");
        }
        boolean deleted = credentials.deleteByIdForAccount(accountId, credentialId);
        if (!deleted) {
            throw new NotFoundException("Credential not found");
        }
    }
}

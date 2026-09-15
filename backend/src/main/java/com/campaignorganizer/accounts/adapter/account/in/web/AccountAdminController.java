package com.campaignorganizer.accounts.adapter.account.in.web;

import com.campaignorganizer.accounts.adapter.account.in.web.AccountWebDtos.AccountResponse;
import com.campaignorganizer.accounts.adapter.account.in.web.AccountWebDtos.ChangePasswordRequest;
import com.campaignorganizer.accounts.adapter.account.in.web.AccountWebDtos.ResetPasswordRequest;
import com.campaignorganizer.accounts.adapter.account.in.web.AccountWebDtos.UpdateRoleRequest;
import com.campaignorganizer.accounts.application.account.port.in.ChangeOwnPasswordUseCase;
import com.campaignorganizer.accounts.application.account.port.in.DeleteAccountUseCase;
import com.campaignorganizer.accounts.application.account.port.in.GetAccountUseCase;
import com.campaignorganizer.accounts.application.account.port.in.ListAccountsUseCase;
import com.campaignorganizer.accounts.application.account.port.in.LogoutAllUseCase;
import com.campaignorganizer.accounts.application.account.port.in.ResetPasswordUseCase;
import com.campaignorganizer.accounts.application.account.port.in.SetEnabledUseCase;
import com.campaignorganizer.accounts.application.account.port.in.UpdateRoleUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.ListWebauthnCredentialsUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.RemoveWebauthnCredentialUseCase;
import com.campaignorganizer.accounts.application.mfa.port.in.ResetMfaUseCase;
import com.campaignorganizer.accounts.application.mfa.port.out.WebAuthnCredentialSummary;
import com.campaignorganizer.security.CurrentUserPort;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Account-roster management. Every write here except the self-service {@code /me} routes
 * requires the ADMIN role — Admin's rights stop at the roster, never another account's content
 * (ADR-0109).
 */
@RestController
@RequestMapping("/api/accounts")
public class AccountAdminController {

    private final ListAccountsUseCase listUseCase;
    private final GetAccountUseCase getUseCase;
    private final UpdateRoleUseCase updateRoleUseCase;
    private final SetEnabledUseCase setEnabledUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
    private final ChangeOwnPasswordUseCase changeOwnPasswordUseCase;
    private final LogoutAllUseCase logoutAllUseCase;
    private final DeleteAccountUseCase deleteUseCase;
    private final ResetMfaUseCase resetMfaUseCase;
    private final ListWebauthnCredentialsUseCase listWebauthnCredentialsUseCase;
    private final RemoveWebauthnCredentialUseCase removeWebauthnCredentialUseCase;
    private final CurrentUserPort currentUser;
    private final AccountWebMapper mapper;

    public AccountAdminController(ListAccountsUseCase listUseCase, GetAccountUseCase getUseCase,
                                  UpdateRoleUseCase updateRoleUseCase, SetEnabledUseCase setEnabledUseCase,
                                  ResetPasswordUseCase resetPasswordUseCase,
                                  ChangeOwnPasswordUseCase changeOwnPasswordUseCase,
                                  LogoutAllUseCase logoutAllUseCase, DeleteAccountUseCase deleteUseCase,
                                  ResetMfaUseCase resetMfaUseCase,
                                  ListWebauthnCredentialsUseCase listWebauthnCredentialsUseCase,
                                  RemoveWebauthnCredentialUseCase removeWebauthnCredentialUseCase,
                                  CurrentUserPort currentUser, AccountWebMapper mapper) {
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.updateRoleUseCase = updateRoleUseCase;
        this.setEnabledUseCase = setEnabledUseCase;
        this.resetPasswordUseCase = resetPasswordUseCase;
        this.changeOwnPasswordUseCase = changeOwnPasswordUseCase;
        this.logoutAllUseCase = logoutAllUseCase;
        this.deleteUseCase = deleteUseCase;
        this.resetMfaUseCase = resetMfaUseCase;
        this.listWebauthnCredentialsUseCase = listWebauthnCredentialsUseCase;
        this.removeWebauthnCredentialUseCase = removeWebauthnCredentialUseCase;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AccountResponse> list() {
        return listUseCase.list().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/me")
    public AccountResponse me() {
        return mapper.toResponse(getUseCase.get(currentUser.currentAccountId()));
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request) {
        changeOwnPasswordUseCase.changeOwnPassword(currentUser.currentAccountId(), request.currentPassword(),
                request.newPassword());
    }

    @PostMapping("/me/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll() {
        logoutAllUseCase.logoutAll(currentUser.currentAccountId());
    }

    /**
     * Self-service passkey management (ADR-0111 follow-up). Adding a passkey has no endpoint
     * here at all — that's Spring Security's own {@code POST /webauthn/register} ceremony,
     * gated by {@code WebAuthnCredentialRepositoryAdapter} to require the caller already hold
     * the MFA factor before adding a second credential to an account.
     */
    @GetMapping("/me/webauthn-credentials")
    public List<WebAuthnCredentialSummary> listWebauthnCredentials() {
        return listWebauthnCredentialsUseCase.listWebauthnCredentials(currentUser.currentAccountId());
    }

    @DeleteMapping("/me/webauthn-credentials/{credentialId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeWebauthnCredential(@PathVariable UUID credentialId) {
        removeWebauthnCredentialUseCase.removeWebauthnCredential(currentUser.currentAccountId(), credentialId);
    }

    @PatchMapping("/{accountId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse updateRole(@PathVariable UUID accountId, @Valid @RequestBody UpdateRoleRequest request) {
        return mapper.toResponse(updateRoleUseCase.updateRole(accountId, request.role()));
    }

    @PostMapping("/{accountId}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse disable(@PathVariable UUID accountId) {
        return mapper.toResponse(setEnabledUseCase.setEnabled(accountId, false));
    }

    @PostMapping("/{accountId}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse enable(@PathVariable UUID accountId) {
        return mapper.toResponse(setEnabledUseCase.setEnabled(accountId, true));
    }

    @PostMapping("/{accountId}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID accountId, @Valid @RequestBody ResetPasswordRequest request) {
        resetPasswordUseCase.resetPassword(accountId, request.newPassword());
    }

    @DeleteMapping("/{accountId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID accountId) {
        deleteUseCase.delete(accountId);
    }

    /**
     * Clears an account's MFA method, secrets, and outstanding recovery codes, forcing
     * re-enrollment on next login (ADR-0111) — the recovery path for a lost device with no
     * codes left, or an enrollment the admin believes wasn't done by the legitimate owner
     * (this app has no out-of-band channel to verify identity beyond the password itself, so
     * a leaked password alone is enough to complete enrollment).
     */
    @PostMapping("/{accountId}/reset-mfa")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse resetMfa(@PathVariable UUID accountId) {
        return mapper.toResponse(resetMfaUseCase.resetMfa(accountId));
    }
}

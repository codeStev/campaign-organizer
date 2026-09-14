package com.campaignorganizer.accounts.adapter.account.out.mfa;

import com.campaignorganizer.accounts.application.account.port.out.AccountRepositoryPort;
import com.campaignorganizer.accounts.domain.account.Account;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Satisfies {@code WebAuthnAuthenticationProvider}'s required constructor dependency — this
 * app has no other use for Spring Security's {@code UserDetailsService}/{@code UserDetails}
 * concept (its real identity model is {@code Account}/{@code CurrentUserPort}). Confirmed by
 * reading the provider's actual source: it calls {@code loadUserByUsername} with the WebAuthn
 * user entity's {@code name} — which this app sets to the account's email at enrollment (see
 * {@link WebAuthnUserEntityRepositoryAdapter}) — purely to decorate the resulting {@code
 * WebAuthnAuthentication} with a role authority Spring itself never forwards anywhere; the
 * actual JWT this app issues after a successful ceremony is built independently, by looking up
 * the account directly (see the WebAuthn success handler in {@code SecurityConfig}). The
 * password field is irrelevant here — WebAuthn doesn't re-check it.
 */
@Component
public class WebAuthnUserDetailsService implements UserDetailsService {

    private final AccountRepositoryPort accounts;

    public WebAuthnUserDetailsService(AccountRepositoryPort accounts) {
        this.accounts = accounts;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Account account = accounts.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new UsernameNotFoundException("No account for email"));
        return User.withUsername(email)
                .password("")
                .authorities("ROLE_" + account.getRole())
                .build();
    }
}

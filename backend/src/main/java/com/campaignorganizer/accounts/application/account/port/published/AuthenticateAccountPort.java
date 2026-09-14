package com.campaignorganizer.accounts.application.account.port.published;

import java.util.Optional;

/**
 * Password verification happens entirely inside this context — the hash
 * never leaves it. Deliberately takes the same time whether the email
 * exists or not (ADR-0110, anti-enumeration): a nonexistent email still
 * runs a dummy hash comparison before returning empty, and a disabled
 * account returns empty the same as a wrong password.
 */
public interface AuthenticateAccountPort {

    Optional<AccountView> authenticate(String email, String rawPassword);
}

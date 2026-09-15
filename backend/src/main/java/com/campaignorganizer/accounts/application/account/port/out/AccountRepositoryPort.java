package com.campaignorganizer.accounts.application.account.port.out;

import com.campaignorganizer.accounts.domain.account.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountRepositoryPort {

    List<Account> findAllOrderByCreatedAtDesc();

    Optional<Account> findById(UUID accountId);

    Optional<Account> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** ADR-0113: looks up an OIDC-authenticated account by its external identity, never by email. */
    Optional<Account> findByProviderAndSubject(String authProvider, String externalSubject);

    long count();

    Account save(Account account);

    void delete(Account account);
}

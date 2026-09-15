package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.application.account.port.out.AccountRepositoryPort;
import com.campaignorganizer.accounts.domain.account.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountRepositoryAdapter implements AccountRepositoryPort {

    private final AccountJpaRepository repository;
    private final AccountPersistenceMapper mapper;

    public AccountRepositoryAdapter(AccountJpaRepository repository, AccountPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Account> findAllOrderByCreatedAtDesc() {
        return repository.findAllByOrderByCreatedAtDesc().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Account> findById(UUID accountId) {
        return repository.findById(accountId).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByEmailIgnoreCase(String email) {
        return repository.findByEmailIgnoreCase(email).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return repository.existsByEmailIgnoreCase(email);
    }

    @Override
    public Optional<Account> findByProviderAndSubject(String authProvider, String externalSubject) {
        return repository.findByAuthProviderAndExternalSubject(authProvider, externalSubject).map(mapper::toDomain);
    }

    @Override
    public long count() {
        return repository.count();
    }

    @Override
    public Account save(Account account) {
        return mapper.toDomain(repository.save(mapper.toEntity(account)));
    }

    @Override
    public void delete(Account account) {
        repository.deleteById(account.getId());
    }
}

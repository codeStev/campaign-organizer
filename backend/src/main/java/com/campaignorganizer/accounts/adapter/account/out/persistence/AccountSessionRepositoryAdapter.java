package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.application.session.port.out.AccountSessionRecord;
import com.campaignorganizer.accounts.application.session.port.out.AccountSessionRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class AccountSessionRepositoryAdapter implements AccountSessionRepositoryPort {

    private final AccountSessionJpaRepository repository;

    public AccountSessionRepositoryAdapter(AccountSessionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public void save(AccountSessionRecord session) {
        AccountSessionJpaEntity entity = new AccountSessionJpaEntity();
        entity.setId(session.id());
        entity.setAccountId(session.accountId());
        entity.setUserAgent(session.userAgent());
        entity.setIpAddress(session.ipAddress());
        entity.setCreatedAt(session.createdAt());
        entity.setExpiresAt(session.expiresAt());
        entity.setRevokedAt(session.revokedAt());
        repository.save(entity);
    }

    @Override
    public Optional<AccountSessionRecord> findById(UUID sessionId) {
        return repository.findById(sessionId).map(AccountSessionRepositoryAdapter::toRecord);
    }

    @Override
    public List<AccountSessionRecord> findByAccountId(UUID accountId) {
        return repository.findByAccountIdOrderByCreatedAtAsc(accountId).stream()
                .map(AccountSessionRepositoryAdapter::toRecord)
                .toList();
    }

    @Override
    public boolean revokeByIdForAccount(UUID accountId, UUID sessionId, Instant revokedAt) {
        return repository.revokeByIdAndAccountId(sessionId, accountId, revokedAt) > 0;
    }

    private static AccountSessionRecord toRecord(AccountSessionJpaEntity entity) {
        return new AccountSessionRecord(entity.getId(), entity.getAccountId(), entity.getCreatedAt(),
                entity.getUserAgent(), entity.getIpAddress(), entity.getExpiresAt(), entity.getRevokedAt());
    }
}

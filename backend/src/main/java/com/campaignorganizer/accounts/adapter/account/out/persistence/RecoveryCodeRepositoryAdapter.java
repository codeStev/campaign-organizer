package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.application.mfa.port.out.RecoveryCodeRepositoryPort;
import com.campaignorganizer.accounts.domain.recoverycode.RecoveryCode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class RecoveryCodeRepositoryAdapter implements RecoveryCodeRepositoryPort {

    private final AccountRecoveryCodeJpaRepository repository;
    private final RecoveryCodePersistenceMapper mapper;

    public RecoveryCodeRepositoryAdapter(AccountRecoveryCodeJpaRepository repository,
                                         RecoveryCodePersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public void saveAll(List<RecoveryCode> codes) {
        repository.saveAll(codes.stream().map(mapper::toEntity).toList());
    }

    @Override
    public void save(RecoveryCode code) {
        repository.save(mapper.toEntity(code));
    }

    @Override
    public List<RecoveryCode> findUnusedByAccountId(UUID accountId) {
        return repository.findByAccountIdAndUsedAtIsNull(accountId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public void deleteAllByAccountId(UUID accountId) {
        repository.deleteAllByAccountId(accountId);
    }
}

package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.application.apitoken.port.out.ApiTokenRepositoryPort;
import com.campaignorganizer.accounts.domain.apitoken.ApiToken;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ApiTokenRepositoryAdapter implements ApiTokenRepositoryPort {

    private final ApiTokenJpaRepository repository;
    private final ApiTokenPersistenceMapper mapper;

    public ApiTokenRepositoryAdapter(ApiTokenJpaRepository repository, ApiTokenPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ApiToken save(ApiToken token) {
        return mapper.toDomain(repository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<ApiToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<ApiToken> findByIdAndAccountId(UUID tokenId, UUID accountId) {
        return repository.findByIdAndAccountId(tokenId, accountId).map(mapper::toDomain);
    }

    @Override
    public List<ApiToken> findActiveByAccountId(UUID accountId) {
        return repository.findByAccountIdAndRevokedAtIsNullOrderByCreatedAtAsc(accountId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}

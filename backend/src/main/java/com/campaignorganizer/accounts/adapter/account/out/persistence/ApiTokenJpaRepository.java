package com.campaignorganizer.accounts.adapter.account.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiTokenJpaRepository extends JpaRepository<ApiTokenJpaEntity, UUID> {

    Optional<ApiTokenJpaEntity> findByTokenHash(String tokenHash);

    Optional<ApiTokenJpaEntity> findByIdAndAccountId(UUID id, UUID accountId);

    List<ApiTokenJpaEntity> findByAccountIdAndRevokedAtIsNullOrderByCreatedAtAsc(UUID accountId);
}

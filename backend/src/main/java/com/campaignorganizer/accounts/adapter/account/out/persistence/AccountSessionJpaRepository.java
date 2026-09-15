package com.campaignorganizer.accounts.adapter.account.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AccountSessionJpaRepository extends JpaRepository<AccountSessionJpaEntity, UUID> {

    List<AccountSessionJpaEntity> findByAccountIdOrderByCreatedAtAsc(UUID accountId);

    /** Scoped by account so a session id from one account can never revoke another's; a no-op on an already-revoked row. */
    @Modifying
    @Query("update AccountSessionJpaEntity s set s.revokedAt = :revokedAt "
            + "where s.id = :id and s.accountId = :accountId and s.revokedAt is null")
    int revokeByIdAndAccountId(UUID id, UUID accountId, Instant revokedAt);
}

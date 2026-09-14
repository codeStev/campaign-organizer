package com.campaignorganizer.accounts.adapter.account.out.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AccountRecoveryCodeJpaRepository extends JpaRepository<AccountRecoveryCodeJpaEntity, UUID> {

    List<AccountRecoveryCodeJpaEntity> findByAccountIdAndUsedAtIsNull(UUID accountId);

    @Modifying
    @Query("delete from AccountRecoveryCodeJpaEntity c where c.accountId = :accountId")
    void deleteAllByAccountId(UUID accountId);
}

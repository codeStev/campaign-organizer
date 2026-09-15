package com.campaignorganizer.accounts.adapter.account.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface WebAuthnCredentialJpaRepository extends JpaRepository<WebAuthnCredentialJpaEntity, UUID> {

    Optional<WebAuthnCredentialJpaEntity> findByCredentialId(byte[] credentialId);

    List<WebAuthnCredentialJpaEntity> findByAccountId(UUID accountId);

    boolean existsByAccountId(UUID accountId);

    @Modifying
    @Query("delete from WebAuthnCredentialJpaEntity c where c.accountId = :accountId")
    void deleteByAccountId(UUID accountId);

    @Modifying
    @Query("delete from WebAuthnCredentialJpaEntity c where c.credentialId = :credentialId")
    void deleteByCredentialId(byte[] credentialId);
}

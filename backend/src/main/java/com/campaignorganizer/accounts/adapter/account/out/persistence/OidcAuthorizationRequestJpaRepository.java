package com.campaignorganizer.accounts.adapter.account.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OidcAuthorizationRequestJpaRepository extends JpaRepository<OidcAuthorizationRequestJpaEntity, String> {
}

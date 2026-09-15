package com.campaignorganizer.accounts.adapter.account.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OidcLoginExchangeJpaRepository extends JpaRepository<OidcLoginExchangeJpaEntity, UUID> {
}

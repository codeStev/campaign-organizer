package com.campaignorganizer.accounts.adapter.account.out.persistence;

import com.campaignorganizer.accounts.application.oidc.port.out.OidcLoginExchangePort;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OidcLoginExchangeRepositoryAdapter implements OidcLoginExchangePort {

    private final OidcLoginExchangeJpaRepository repository;
    private final Clock clock;

    public OidcLoginExchangeRepositoryAdapter(OidcLoginExchangeJpaRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public UUID stage(String loginResponseJson, Instant expiresAt) {
        OidcLoginExchangeJpaEntity entity = new OidcLoginExchangeJpaEntity();
        entity.setCode(UUID.randomUUID());
        entity.setLoginResponseJson(loginResponseJson);
        entity.setExpiresAt(expiresAt);
        repository.save(entity);
        return entity.getCode();
    }

    @Override
    @Transactional
    public Optional<String> consume(UUID code) {
        return repository.findById(code)
                .filter(entity -> entity.getExpiresAt().isAfter(clock.instant()))
                .map(entity -> {
                    repository.deleteById(code);
                    return entity.getLoginResponseJson();
                });
    }
}

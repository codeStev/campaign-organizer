package com.campaignorganizer.accounts.application.apitoken.service;

import com.campaignorganizer.accounts.application.apitoken.port.in.CreateApiTokenUseCase;
import com.campaignorganizer.accounts.application.apitoken.port.in.ListApiTokensUseCase;
import com.campaignorganizer.accounts.application.apitoken.port.in.RevokeApiTokenUseCase;
import com.campaignorganizer.accounts.application.apitoken.port.out.ApiTokenRepositoryPort;
import com.campaignorganizer.accounts.application.apitoken.port.out.ApiTokenSecretPort;
import com.campaignorganizer.accounts.application.apitoken.port.out.ApiTokenSecretPort.GeneratedSecret;
import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenQueryPort;
import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenView;
import com.campaignorganizer.accounts.domain.apitoken.ApiToken;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** API token use cases (ADR-0119); also implements the published query port used by the auth filter. */
@Service
public class ApiTokenService implements CreateApiTokenUseCase, ListApiTokensUseCase, RevokeApiTokenUseCase,
        ApiTokenQueryPort {

    private final ApiTokenRepositoryPort tokens;
    private final ApiTokenSecretPort secrets;
    private final ApiTokenViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public ApiTokenService(ApiTokenRepositoryPort tokens, ApiTokenSecretPort secrets,
                           ApiTokenViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.tokens = tokens;
        this.secrets = secrets;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public CreatedApiToken create(CreateApiTokenCommand command) {
        GeneratedSecret generated = secrets.generate();
        ApiToken token = ApiToken.create(ids.newId(), command.accountId(), command.name(), generated.hash(),
                generated.prefix(), command.scopes(), command.expiresAt(), clock.instant());
        return new CreatedApiToken(generated.secret(), viewMapper.toView(tokens.save(token)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApiTokenView> list(UUID accountId) {
        return tokens.findActiveByAccountId(accountId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional
    public void revoke(UUID accountId, UUID tokenId) {
        ApiToken token = tokens.findByIdAndAccountId(tokenId, accountId)
                .orElseThrow(() -> new NotFoundException("API token not found"));
        token.revoke(clock.instant());
        tokens.save(token);
    }

    @Override
    @Transactional
    public Optional<ApiTokenView> authenticate(String secret) {
        Instant now = clock.instant();
        return tokens.findByTokenHash(secrets.hash(secret))
                .filter(token -> token.isUsable(now))
                .map(token -> {
                    if (token.recordUse(now)) {
                        tokens.save(token);
                    }
                    return viewMapper.toView(token);
                });
    }
}

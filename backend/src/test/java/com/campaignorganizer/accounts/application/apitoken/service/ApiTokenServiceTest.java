package com.campaignorganizer.accounts.application.apitoken.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.accounts.application.apitoken.port.in.CreateApiTokenUseCase.CreateApiTokenCommand;
import com.campaignorganizer.accounts.application.apitoken.port.in.CreateApiTokenUseCase.CreatedApiToken;
import com.campaignorganizer.accounts.application.apitoken.port.out.ApiTokenRepositoryPort;
import com.campaignorganizer.accounts.application.apitoken.port.out.ApiTokenSecretPort;
import com.campaignorganizer.accounts.application.apitoken.port.out.ApiTokenSecretPort.GeneratedSecret;
import com.campaignorganizer.accounts.application.apitoken.port.published.ApiTokenView;
import com.campaignorganizer.accounts.domain.apitoken.ApiToken;
import com.campaignorganizer.accounts.domain.apitoken.ApiTokenScope;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Unit coverage for the API token use cases (ADR-0119). */
@ExtendWith(MockitoExtension.class)
class ApiTokenServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-23T10:00:00Z");

    @Mock
    private ApiTokenRepositoryPort tokens;
    @Mock
    private ApiTokenSecretPort secrets;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final UUID accountId = UUID.randomUUID();
    private ApiTokenService service;

    @BeforeEach
    void setUp() {
        service = new ApiTokenService(tokens, secrets, new ApiTokenViewMapperImpl(), ids, clock);
    }

    private ApiToken stored(Instant expiresAt) {
        return ApiToken.reconstitute(UUID.randomUUID(), accountId, "recorder", "hash", "cot_abcd",
                Set.of(ApiTokenScope.GLOSSARY_READ), NOW.minusSeconds(3600), expiresAt, null, null);
    }

    @Test
    void creatingReturnsTheSecretOnceAndStoresOnlyItsHash() {
        UUID id = UUID.randomUUID();
        when(ids.newId()).thenReturn(id);
        when(secrets.generate()).thenReturn(new GeneratedSecret("cot_secret", "the-hash", "cot_secre"));
        when(tokens.save(any(ApiToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreatedApiToken created = service.create(new CreateApiTokenCommand(accountId, "recorder",
                Set.of(ApiTokenScope.GLOSSARY_READ), null));

        assertThat(created.secret()).isEqualTo("cot_secret");
        assertThat(created.token()).extracting(ApiTokenView::id, ApiTokenView::accountId, ApiTokenView::prefix)
                .containsExactly(id, accountId, "cot_secre");
        verify(tokens).save(any(ApiToken.class));
    }

    @Test
    void listsTheAccountsActiveTokens() {
        ApiToken token = stored(null);
        when(tokens.findActiveByAccountId(accountId)).thenReturn(List.of(token));

        assertThat(service.list(accountId)).extracting(ApiTokenView::id).containsExactly(token.getId());
    }

    @Test
    void revokingMarksTheTokenRevokedAndSavesIt() {
        ApiToken token = stored(null);
        when(tokens.findByIdAndAccountId(token.getId(), accountId)).thenReturn(Optional.of(token));

        service.revoke(accountId, token.getId());

        assertThat(token.getRevokedAt()).isEqualTo(NOW);
        verify(tokens).save(token);
    }

    @Test
    void revokingAnotherAccountsOrUnknownTokenIsNotFound() {
        UUID tokenId = UUID.randomUUID();
        when(tokens.findByIdAndAccountId(tokenId, accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(accountId, tokenId)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void authenticatingAUsableTokenRecordsItsUse() {
        ApiToken token = stored(NOW.plusSeconds(60));
        when(secrets.hash("cot_secret")).thenReturn("hash");
        when(tokens.findByTokenHash("hash")).thenReturn(Optional.of(token));

        Optional<ApiTokenView> view = service.authenticate("cot_secret");

        assertThat(view).map(ApiTokenView::accountId).contains(accountId);
        verify(tokens).save(token);
    }

    @Test
    void authenticatingARevokedOrExpiredOrUnknownTokenYieldsNothing() {
        ApiToken revoked = stored(null);
        revoked.revoke(NOW.minusSeconds(1));
        ApiToken expired = stored(NOW.minusSeconds(1));
        when(secrets.hash("revoked")).thenReturn("h1");
        when(secrets.hash("expired")).thenReturn("h2");
        when(secrets.hash("unknown")).thenReturn("h3");
        when(tokens.findByTokenHash("h1")).thenReturn(Optional.of(revoked));
        when(tokens.findByTokenHash("h2")).thenReturn(Optional.of(expired));
        when(tokens.findByTokenHash("h3")).thenReturn(Optional.empty());

        assertThat(service.authenticate("revoked")).isEmpty();
        assertThat(service.authenticate("expired")).isEmpty();
        assertThat(service.authenticate("unknown")).isEmpty();
        verify(tokens, never()).save(any(ApiToken.class));
    }
}

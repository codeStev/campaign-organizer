package com.campaignorganizer.accounts.adapter.account.out.mfa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.accounts.application.account.port.published.AccountQueryPort;
import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.session.port.in.RecordAccountSessionUseCase;
import com.campaignorganizer.accounts.domain.account.MfaMethod;
import com.campaignorganizer.accounts.domain.account.Role;
import com.campaignorganizer.security.JwtService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity;
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthentication;

/**
 * Unit coverage for the mfaMethod == WEBAUTHN guard added during this feature's own
 * security-review pass (see the class Javadoc on {@link WebAuthnAuthenticationSuccessHandler}).
 */
@ExtendWith(MockitoExtension.class)
class WebAuthnAuthenticationSuccessHandlerTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private AccountQueryPort accounts;
    @Mock
    private RecordAccountSessionUseCase recordAccountSessionUseCase;

    private final UUID accountId = UUID.randomUUID();

    @Test
    void issuesFullTokenWhenWebauthnIsTheAccountsActiveMfaMethod() throws Exception {
        WebAuthnAuthenticationSuccessHandler handler =
                new WebAuthnAuthenticationSuccessHandler(jwtService, accounts, recordAccountSessionUseCase);
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.WEBAUTHN)));
        UUID jti = UUID.randomUUID();
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(jwtService.issue(accountId, Role.USER, 0))
                .thenReturn(new JwtService.IssuedToken("token-value", expiresAt, jti));
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, webAuthnAuthentication());

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("token-value");
        verify(recordAccountSessionUseCase).recordSession(accountId, jti, expiresAt, null, "127.0.0.1");
    }

    @Test
    void rejectsAnAccountWhoseActiveMfaMethodIsNotWebauthn() {
        WebAuthnAuthenticationSuccessHandler handler =
                new WebAuthnAuthenticationSuccessHandler(jwtService, accounts, recordAccountSessionUseCase);
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.TOTP)));

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(
                new MockHttpServletRequest(), new MockHttpServletResponse(), webAuthnAuthentication()))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rejectsAnAccountWithNoMfaMethodActiveAtAll() {
        WebAuthnAuthenticationSuccessHandler handler =
                new WebAuthnAuthenticationSuccessHandler(jwtService, accounts, recordAccountSessionUseCase);
        when(accounts.findById(accountId)).thenReturn(Optional.of(accountView(MfaMethod.NONE)));

        assertThatThrownBy(() -> handler.onAuthenticationSuccess(
                new MockHttpServletRequest(), new MockHttpServletResponse(), webAuthnAuthentication()))
                .isInstanceOf(AccessDeniedException.class);
    }

    private WebAuthnAuthentication webAuthnAuthentication() {
        PublicKeyCredentialUserEntity userEntity = ImmutablePublicKeyCredentialUserEntity.builder()
                .id(WebAuthnUserHandle.toBytes(accountId))
                .name("gm@example.com")
                .displayName("gm@example.com")
                .build();
        return new WebAuthnAuthentication(userEntity, List.of());
    }

    private AccountView accountView(MfaMethod mfaMethod) {
        return new AccountView(accountId, "gm@example.com", Role.USER, true, 0, mfaMethod, Instant.now());
    }
}

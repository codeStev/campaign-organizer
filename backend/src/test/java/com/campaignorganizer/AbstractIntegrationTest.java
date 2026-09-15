package com.campaignorganizer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.campaignorganizer.accounts.application.account.port.published.AccountView;
import com.campaignorganizer.accounts.application.account.port.published.AuthenticateAccountPort;
import com.campaignorganizer.accounts.application.session.port.in.RecordAccountSessionUseCase;
import com.campaignorganizer.security.JwtService;
import com.jayway.jsonpath.JsonPath;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for HTTP-level integration tests. Boots the full application
 * against one shared PostgreSQL container so Flyway migrations and JPA mapping
 * are exercised exactly as in production.
 *
 * <p>The container is a JVM-lifetime <b>singleton</b> started in a static
 * initializer (not via {@code @Testcontainers}/{@code @Container}). This keeps
 * it stable across the cached Spring context that all integration tests share;
 * a per-class container would be restarted underneath the cached context.
 *
 * <p>Payloads are built as plain JSON strings and responses inspected with
 * JsonPath to stay independent of which Jackson version Spring wires.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    private AuthenticateAccountPort authenticateAccountPort;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RecordAccountSessionUseCase recordAccountSessionUseCase;

    /**
     * Registers a fresh, unique account and returns a ready, fully-authenticated
     * {@code Bearer <jwt>} header value — bypassing the real MFA enrollment ceremony
     * (ADR-0111) by minting the token directly via {@link JwtService} in-process, since the
     * vast majority of callers in this suite are testing something else entirely and would
     * otherwise all need to carry the TOTP setup/confirm dance.
     * {@code com.campaignorganizer.accounts.MfaControllerIT} exercises that real flow
     * end-to-end via HTTP. A new account per call is deliberate —
     * every caller in this suite uses the header within a single test method, never expecting
     * a *specific* identity across calls, and each account's data is private to it
     * (ADR-0109), so reusing one shared login across tests would only entangle them.
     *
     * <p>Also records a session row (ADR-0112) — a full-factor token now needs one, exactly
     * like every real full-token-issuance call site, or {@code JwtAuthFilter}'s active-session
     * check rejects even this test shortcut's own token.
     */
    protected String authHeader() throws Exception {
        String email = "it-" + java.util.UUID.randomUUID() + "@test.local";
        String password = "integration-test-password";
        mockMvc.perform(post("/api/accounts/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
        AccountView account = authenticateAccountPort.authenticate(email, password).orElseThrow();
        JwtService.IssuedToken issued = jwtService.issue(account.id(), account.role(), account.tokenVersion());
        recordAccountSessionUseCase.recordSession(account.id(), issued.jti(), issued.expiresAt(),
                "integration-test", "127.0.0.1");
        return "Bearer " + issued.token();
    }

    /** Creates a world and returns its id. */
    protected String createWorld(String auth) throws Exception {
        String body = mockMvc.perform(post("/api/worlds")
                        .header(HttpHeaders.AUTHORIZATION, auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test World\"}"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    /** Uploads a tiny PNG into the world and returns the media id. */
    protected String uploadImage(String auth, String worldId) throws Exception {
        var file = new MockMultipartFile("file", "map.png", "image/png",
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3, 4});
        String body = mockMvc.perform(multipart("/api/worlds/{w}/media", worldId)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, auth))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }
}

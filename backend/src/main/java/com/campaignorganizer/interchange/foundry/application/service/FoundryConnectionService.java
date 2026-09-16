package com.campaignorganizer.interchange.foundry.application.service;

import com.campaignorganizer.interchange.foundry.application.port.in.FoundryConnectionView;
import com.campaignorganizer.interchange.foundry.application.port.in.GetFoundryConnectionUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.SaveFoundryConnectionUseCase;
import com.campaignorganizer.interchange.foundry.application.port.in.TestFoundryConnectionUseCase;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryConnectionRepositoryPort;
import com.campaignorganizer.interchange.foundry.application.port.out.FoundryRelayPort;
import com.campaignorganizer.interchange.foundry.domain.FoundryConnection;
import com.campaignorganizer.interchange.foundry.domain.FoundryRelayException;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.world.port.published.WorldQueryPort;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Per-world Foundry relay connection settings (ADR-0115) — pure composition
 * over {@code worldbuilding}'s published {@link WorldQueryPort} plus this
 * context's own connection storage, mirroring {@code CampaignCalendarService}'s
 * style. The API key is encrypted/decrypted here, in the application layer —
 * never in the domain or persistence adapter. */
@Service
public class FoundryConnectionService implements GetFoundryConnectionUseCase, SaveFoundryConnectionUseCase,
        TestFoundryConnectionUseCase {

    private final FoundryConnectionRepositoryPort connections;
    private final WorldQueryPort worlds;
    private final FoundryRelayPort relay;
    private final TextEncryptor apiKeyEncryptor;
    private final Clock clock;

    public FoundryConnectionService(FoundryConnectionRepositoryPort connections, WorldQueryPort worlds,
                                    FoundryRelayPort relay,
                                    @Qualifier("foundryApiKeyEncryptor") TextEncryptor apiKeyEncryptor,
                                    Clock clock) {
        this.connections = connections;
        this.worlds = worlds;
        this.relay = relay;
        this.apiKeyEncryptor = apiKeyEncryptor;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FoundryConnectionView> get(UUID worldId) {
        requireWorld(worldId);
        return connections.findByWorldId(worldId).map(this::toView);
    }

    @Override
    @Transactional
    public FoundryConnectionView save(UUID worldId, SaveFoundryConnectionCommand command) {
        requireWorld(worldId);
        Optional<FoundryConnection> existing = connections.findByWorldId(worldId);
        String encryptedKey = command.apiKey()
                .filter(key -> !key.isBlank())
                .map(apiKeyEncryptor::encrypt)
                .or(() -> existing.map(FoundryConnection::getApiKeyEncrypted))
                .orElseThrow(() -> new ValidationException("Foundry API key is required"));
        Instant now = clock.instant();
        FoundryConnection connection = existing
                .map(c -> {
                    c.update(command.relayBaseUrl(), command.clientId(), encryptedKey, now);
                    return c;
                })
                .orElseGet(() -> FoundryConnection.create(worldId, command.relayBaseUrl(), command.clientId(),
                        encryptedKey, now));
        return toView(connections.save(connection));
    }

    @Override
    @Transactional(readOnly = true)
    public FoundryConnectionTestView test(UUID worldId) {
        requireWorld(worldId);
        Optional<FoundryConnection> connection = connections.findByWorldId(worldId);
        if (connection.isEmpty()) {
            return new FoundryConnectionTestView(false, List.of(),
                    "No Foundry connection configured for this world");
        }
        FoundryConnection c = connection.get();
        try {
            List<String> connected = relay.listConnectedClients(credentialsFor(c));
            return new FoundryConnectionTestView(connected.contains(c.getClientId()), connected, null);
        } catch (FoundryRelayException e) {
            return new FoundryConnectionTestView(false, List.of(), e.getMessage());
        }
    }

    private FoundryRelayPort.Credentials credentialsFor(FoundryConnection c) {
        return new FoundryRelayPort.Credentials(c.getRelayBaseUrl(), apiKeyEncryptor.decrypt(c.getApiKeyEncrypted()),
                c.getClientId());
    }

    private FoundryConnectionView toView(FoundryConnection c) {
        return new FoundryConnectionView(c.getWorldId(), c.getRelayBaseUrl(), c.getClientId(), true);
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}

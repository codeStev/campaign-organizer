package com.campaignorganizer.campaign.application.player.service;

import com.campaignorganizer.campaign.application.player.port.in.CreatePlayerUseCase;
import com.campaignorganizer.campaign.application.player.port.in.DeletePlayerUseCase;
import com.campaignorganizer.campaign.application.player.port.in.GetPlayerUseCase;
import com.campaignorganizer.campaign.application.player.port.in.ListPlayersUseCase;
import com.campaignorganizer.campaign.application.player.port.in.PlayerCommands.CreatePlayerCommand;
import com.campaignorganizer.campaign.application.player.port.in.PlayerCommands.UpdatePlayerCommand;
import com.campaignorganizer.campaign.application.player.port.in.UpdatePlayerUseCase;
import com.campaignorganizer.campaign.application.player.port.out.PlayerRepositoryPort;
import com.campaignorganizer.campaign.application.player.port.out.WorldExistsPort;
import com.campaignorganizer.campaign.application.player.port.published.PlayerImportPort;
import com.campaignorganizer.campaign.application.player.port.published.PlayerQueryPort;
import com.campaignorganizer.campaign.application.player.port.published.PlayerView;
import com.campaignorganizer.campaign.domain.player.Player;
import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Player use cases; also implements the published query port for consumers. */
@Service
public class PlayerService implements CreatePlayerUseCase, UpdatePlayerUseCase,
        DeletePlayerUseCase, GetPlayerUseCase, ListPlayersUseCase, PlayerQueryPort,
        PlayerImportPort {

    private final PlayerRepositoryPort players;
    private final WorldExistsPort worlds;
    private final PlayerViewMapper viewMapper;
    private final IdGenerator ids;
    private final Clock clock;

    public PlayerService(PlayerRepositoryPort players, WorldExistsPort worlds,
                         PlayerViewMapper viewMapper, IdGenerator ids, Clock clock) {
        this.players = players;
        this.worlds = worlds;
        this.viewMapper = viewMapper;
        this.ids = ids;
        this.clock = clock;
    }

    @Override
    @Transactional
    public PlayerView create(CreatePlayerCommand command) {
        requireWorld(command.worldId());
        Player created = Player.create(ids.newId(), command.worldId(), command.name(), clock.instant());
        return viewMapper.toView(players.save(created));
    }

    @Override
    @Transactional
    public PlayerView update(UpdatePlayerCommand command) {
        Player player = require(command.worldId(), command.playerId());
        player.update(command.name(), clock.instant());
        return viewMapper.toView(players.save(player));
    }

    @Override
    @Transactional
    public void delete(UUID worldId, UUID playerId) {
        players.delete(require(worldId, playerId));
    }

    @Override
    @Transactional(readOnly = true)
    public PlayerView get(UUID worldId, UUID playerId) {
        return viewMapper.toView(require(worldId, playerId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlayerView> list(UUID worldId) {
        requireWorld(worldId);
        return findByWorld(worldId);
    }

    // --- published import port (ADR-0061) ---

    @Override
    @Transactional
    public PlayerView importPlayer(PlayerView view) {
        Player player = Player.reconstitute(view.id(), view.worldId(), view.name(), view.createdAt(),
                view.updatedAt());
        return viewMapper.toView(players.save(player));
    }

    // --- published query port ---

    @Override
    @Transactional(readOnly = true)
    public List<PlayerView> findByWorld(UUID worldId) {
        return players.findByWorld(worldId).stream().map(viewMapper::toView).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlayerView> findByIdInWorld(UUID playerId, UUID worldId) {
        return players.findByIdAndWorld(playerId, worldId).map(viewMapper::toView);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsInWorld(UUID playerId, UUID worldId) {
        return players.existsInWorld(playerId, worldId);
    }

    private Player require(UUID worldId, UUID playerId) {
        return players.findByIdAndWorld(playerId, worldId)
                .orElseThrow(() -> new NotFoundException("Player not found"));
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.exists(worldId)) {
            throw new NotFoundException("World not found");
        }
    }
}

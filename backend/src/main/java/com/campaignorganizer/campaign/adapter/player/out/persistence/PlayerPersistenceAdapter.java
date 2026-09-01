package com.campaignorganizer.campaign.adapter.player.out.persistence;

import com.campaignorganizer.campaign.application.player.port.out.PlayerRepositoryPort;
import com.campaignorganizer.campaign.domain.player.Player;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PlayerPersistenceAdapter implements PlayerRepositoryPort {

    private final PlayerJpaRepository repository;
    private final PlayerPersistenceMapper mapper;

    public PlayerPersistenceAdapter(PlayerJpaRepository repository, PlayerPersistenceMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public List<Player> findByWorld(UUID worldId) {
        return repository.findByWorldIdOrderByNameAsc(worldId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Player> findByIdAndWorld(UUID playerId, UUID worldId) {
        return repository.findByIdAndWorldId(playerId, worldId).map(mapper::toDomain);
    }

    @Override
    public Optional<Player> findById(UUID playerId) {
        return repository.findById(playerId).map(mapper::toDomain);
    }

    @Override
    public boolean existsInWorld(UUID playerId, UUID worldId) {
        return repository.existsByIdAndWorldId(playerId, worldId);
    }

    @Override
    public Player save(Player player) {
        return mapper.toDomain(repository.save(mapper.toEntity(player)));
    }

    @Override
    public void delete(Player player) {
        repository.deleteById(player.getId());
    }
}

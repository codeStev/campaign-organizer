package com.campaignorganizer.characters.adapter.template.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameSystemJpaRepository extends JpaRepository<GameSystemJpaEntity, UUID> {

    List<GameSystemJpaEntity> findAllByOrderByNameAsc();

    Optional<GameSystemJpaEntity> findByNameIgnoreCase(String name);
}

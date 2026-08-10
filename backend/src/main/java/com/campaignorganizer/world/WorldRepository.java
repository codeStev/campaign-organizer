package com.campaignorganizer.world;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorldRepository extends JpaRepository<World, UUID> {

    List<World> findAllByOrderByCreatedAtDesc();
}

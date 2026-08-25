package com.campaignorganizer.ai.adapter.out.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiProviderSettingsJpaRepository extends JpaRepository<AiProviderSettingsJpaEntity, String> {

    List<AiProviderSettingsJpaEntity> findAllByOrderByPriorityAsc();
}

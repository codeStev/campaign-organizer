package com.campaignorganizer.campaign.application.loosethread.port.out;

import com.campaignorganizer.campaign.domain.loosethread.LooseThread;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LooseThreadRepositoryPort {

    List<LooseThread> findBySession(UUID sessionId);

    List<LooseThread> findByCampaign(UUID campaignId);

    Optional<LooseThread> findByIdAndSession(UUID threadId, UUID sessionId);

    Optional<LooseThread> findById(UUID threadId);

    LooseThread save(LooseThread thread);

    void delete(LooseThread thread);
}

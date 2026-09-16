package com.campaignorganizer.interchange.foundry.adapter.out.persistence;

import com.campaignorganizer.interchange.foundry.domain.FoundryConnection;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FoundryConnectionPersistenceMapper {

    FoundryConnectionJpaEntity toEntity(FoundryConnection connection);

    default FoundryConnection toDomain(FoundryConnectionJpaEntity e) {
        if (e == null) {
            return null;
        }
        return FoundryConnection.reconstitute(e.getWorldId(), e.getRelayBaseUrl(), e.getClientId(),
                e.getApiKeyEncrypted(), e.getCreatedAt(), e.getUpdatedAt());
    }
}

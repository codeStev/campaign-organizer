package com.campaignorganizer.media.adapter.out.persistence;

import com.campaignorganizer.media.domain.MediaAsset;
import org.mapstruct.Mapper;

/** Maps the domain asset to/from its JPA entity (MapStruct). */
@Mapper(componentModel = "spring")
public interface MediaPersistenceMapper {

    MediaJpaEntity toEntity(MediaAsset asset);

    default MediaAsset toDomain(MediaJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return MediaAsset.reconstitute(
                entity.getId(),
                entity.getWorldId(),
                entity.getFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getStorageKey(),
                entity.getCreatedAt());
    }
}

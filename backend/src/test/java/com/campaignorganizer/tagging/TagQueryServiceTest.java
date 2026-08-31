package com.campaignorganizer.tagging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.campaignorganizer.tagging.application.port.out.EntityTagRepositoryPort;
import com.campaignorganizer.tagging.application.service.TagQueryService;
import com.campaignorganizer.tagging.application.service.TagViewMapper;
import com.campaignorganizer.tagging.domain.EntityType;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Deliberately depends only on the repository port — see {@link TagQueryService}'s Javadoc. */
@ExtendWith(MockitoExtension.class)
class TagQueryServiceTest {

    private final UUID worldId = UUID.randomUUID();

    @Mock
    private EntityTagRepositoryPort repo;

    private TagQueryService service;

    @BeforeEach
    void setUp() {
        service = new TagQueryService(repo, Mappers.getMapper(TagViewMapper.class));
    }

    @Test
    void entityIdsTaggedWithNormalizesTheQueryName() {
        service.entityIdsTaggedWith(worldId, EntityType.ARTICLE, "  Villain ");

        verify(repo).findEntityIdsByWorldAndTypeAndName(worldId, EntityType.ARTICLE, "villain");
    }

    @Test
    void findByWorldDelegatesToTheRepository() {
        service.findByWorld(worldId);

        verify(repo).findByWorld(any());
    }
}

package com.campaignorganizer.tagging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.tagging.application.port.in.TagCommands.SetEntityTagsCommand;
import com.campaignorganizer.tagging.application.port.out.ArticleExistsPort;
import com.campaignorganizer.tagging.application.port.out.EntityTagRepositoryPort;
import com.campaignorganizer.tagging.application.port.out.StatblockExistsPort;
import com.campaignorganizer.tagging.application.port.out.WorldExistsPort;
import com.campaignorganizer.tagging.application.port.published.TagQueryPort;
import com.campaignorganizer.tagging.application.service.TagViewMapper;
import com.campaignorganizer.tagging.application.service.TaggingService;
import com.campaignorganizer.tagging.domain.EntityTag;
import com.campaignorganizer.tagging.domain.EntityType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tag use cases against mocked out-ports; whole-set replace and normalization are the rules under test. */
@ExtendWith(MockitoExtension.class)
class TaggingServiceTest {

    private final UUID worldId = UUID.randomUUID();
    private final UUID articleId = UUID.randomUUID();
    private final Instant now = Instant.EPOCH;

    @Mock
    private EntityTagRepositoryPort repo;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private ArticleExistsPort articles;
    @Mock
    private StatblockExistsPort statblocks;
    @Mock
    private TagQueryPort tagQuery;
    @Mock
    private IdGenerator ids;

    private TaggingService service;

    @BeforeEach
    void setUp() {
        lenient().when(worlds.exists(worldId)).thenReturn(true);
        lenient().when(articles.existsInWorld(any(), any())).thenReturn(true);
        lenient().when(ids.newId()).thenReturn(UUID.randomUUID());
        lenient().when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new TaggingService(repo, worlds, articles, statblocks, tagQuery,
                Mappers.getMapper(TagViewMapper.class), ids,
                Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void setRejectsUnknownWorld() {
        when(worlds.exists(any())).thenReturn(false);
        assertThatThrownBy(() -> service.set(
                new SetEntityTagsCommand(worldId, EntityType.ARTICLE, articleId, Set.of("npc"))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void setRejectsUnknownEntity() {
        when(articles.existsInWorld(any(), any())).thenReturn(false);
        assertThatThrownBy(() -> service.set(
                new SetEntityTagsCommand(worldId, EntityType.ARTICLE, articleId, Set.of("npc"))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void setReplacesTheWholeTagSetNormalizedAndSorted() {
        List<String> result = service.set(new SetEntityTagsCommand(worldId, EntityType.ARTICLE,
                articleId, Set.of("  Villain ", "Session 1", "villain")));

        assertThat(result).containsExactly("session 1", "villain");
        verify(repo).deleteByEntity(worldId, EntityType.ARTICLE, articleId);
        verify(repo, times(2)).save(any(EntityTag.class));
    }

    @Test
    void setWithEmptySetClearsAllTags() {
        List<String> result = service.set(
                new SetEntityTagsCommand(worldId, EntityType.ARTICLE, articleId, Set.of()));

        assertThat(result).isEmpty();
        verify(repo).deleteByEntity(worldId, EntityType.ARTICLE, articleId);
        verify(repo, never()).save(any());
    }

    @Test
    void listDelegatesToTheQueryPort() {
        when(tagQuery.tagsFor(worldId, EntityType.ARTICLE, articleId)).thenReturn(List.of("npc"));

        assertThat(service.list(worldId, EntityType.ARTICLE, articleId)).containsExactly("npc");
    }

    @Test
    void listRequiresTheWorldToExist() {
        when(worlds.exists(any())).thenReturn(false);
        assertThatThrownBy(() -> service.list(worldId)).isInstanceOf(NotFoundException.class);
    }
}

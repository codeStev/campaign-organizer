package com.campaignorganizer.worldbuilding.application.wiki.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.campaignorganizer.shared.application.IdGenerator;
import com.campaignorganizer.shared.domain.NotFoundException;
import com.campaignorganizer.shared.domain.ValidationException;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CategoryCommands.CreateCategoryCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.in.CategoryCommands.UpdateCategoryCommand;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.CategoryRepositoryPort;
import com.campaignorganizer.worldbuilding.application.wiki.port.out.WorldExistsPort;
import com.campaignorganizer.worldbuilding.domain.wiki.Category;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Application service unit test for categories with mocked ports. */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepositoryPort categories;
    @Mock
    private WorldExistsPort worlds;
    @Mock
    private IdGenerator ids;

    private final Clock clock = Clock.fixed(Instant.parse("2026-03-03T00:00:00Z"), ZoneOffset.UTC);
    private final CategoryViewMapper viewMapper = new CategoryViewMapperImpl();

    private CategoryService service;

    private final UUID worldId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new CategoryService(categories, worlds, viewMapper, ids, clock);
    }

    @Test
    void createRejectsMissingWorld() {
        when(worlds.exists(worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateCategoryCommand(worldId, null, "People")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsForeignParent() {
        UUID parent = UUID.randomUUID();
        when(worlds.exists(worldId)).thenReturn(true);
        when(categories.existsInWorld(parent, worldId)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateCategoryCommand(worldId, parent, "People")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void updateRejectsSelfParent() {
        UUID categoryId = UUID.randomUUID();
        when(categories.findByIdAndWorld(categoryId, worldId))
                .thenReturn(Optional.of(Category.create(categoryId, worldId, null, "People", clock.instant())));

        assertThatThrownBy(() -> service.update(
                new UpdateCategoryCommand(worldId, categoryId, categoryId, "People")))
                .isInstanceOf(ValidationException.class);
    }
}

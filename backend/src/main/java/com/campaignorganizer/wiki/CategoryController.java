package com.campaignorganizer.wiki;

import com.campaignorganizer.wiki.CategoryDtos.CategoryRequest;
import com.campaignorganizer.wiki.CategoryDtos.CategoryResponse;
import com.campaignorganizer.world.WorldRepository;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/categories")
public class CategoryController {

    private final CategoryRepository categories;
    private final WorldRepository worlds;

    public CategoryController(CategoryRepository categories, WorldRepository worlds) {
        this.categories = categories;
        this.worlds = worlds;
    }

    @GetMapping
    public List<CategoryResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return categories.findByWorldIdOrderByNameAsc(worldId).stream()
                .map(CategoryResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@PathVariable UUID worldId,
                                                   @Valid @RequestBody CategoryRequest request) {
        requireWorld(worldId);
        validateParent(worldId, request.parentId(), null);
        Category saved = categories.save(new Category(worldId, request.parentId(), request.name()));
        return ResponseEntity
                .created(URI.create("/api/worlds/" + worldId + "/categories/" + saved.getId()))
                .body(CategoryResponse.from(saved));
    }

    @PutMapping("/{categoryId}")
    public CategoryResponse update(@PathVariable UUID worldId, @PathVariable UUID categoryId,
                                   @Valid @RequestBody CategoryRequest request) {
        Category category = categories.findByIdAndWorldId(categoryId, worldId)
                .orElseThrow(this::notFound);
        validateParent(worldId, request.parentId(), categoryId);
        category.update(request.parentId(), request.name());
        return CategoryResponse.from(categories.save(category));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID categoryId) {
        Category category = categories.findByIdAndWorldId(categoryId, worldId)
                .orElseThrow(this::notFound);
        categories.delete(category);
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw notFound();
        }
    }

    private void validateParent(UUID worldId, UUID parentId, UUID selfId) {
        if (parentId == null) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A category cannot be its own parent");
        }
        if (!categories.existsByIdAndWorldId(parentId, worldId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parent category not found in this world");
        }
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Category or world not found");
    }
}

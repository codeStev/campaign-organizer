package com.campaignorganizer.media;

import com.campaignorganizer.world.WorldRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/worlds/{worldId}/media")
public class MediaController {

    private static final long MAX_SIZE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/svg+xml");

    private final MediaRepository media;
    private final MediaStorage storage;
    private final WorldRepository worlds;

    public MediaController(MediaRepository media, MediaStorage storage, WorldRepository worlds) {
        this.media = media;
        this.storage = storage;
        this.worlds = worlds;
    }

    @GetMapping
    public List<MediaResponse> list(@PathVariable UUID worldId) {
        requireWorld(worldId);
        return media.findByWorldIdOrderByCreatedAtDesc(worldId).stream()
                .map(MediaResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<MediaResponse> upload(@PathVariable UUID worldId,
                                                @RequestParam("file") MultipartFile file) {
        requireWorld(worldId);
        validate(file);
        String key = storage.store(readBytes(file));
        MediaAsset saved = media.save(new MediaAsset(
                worldId, safeFilename(file), file.getContentType(), file.getSize(), key));
        return ResponseEntity.status(HttpStatus.CREATED).body(MediaResponse.from(saved));
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID mediaId) {
        MediaAsset asset = media.findByIdAndWorldId(mediaId, worldId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        media.delete(asset);
        storage.delete(asset.getStorageKey());
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Empty file");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File exceeds 10MB limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image uploads are allowed");
        }
    }

    private void requireWorld(UUID worldId) {
        if (!worlds.existsById(worldId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "World not found");
        }
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read upload", e);
        }
    }

    private static String safeFilename(MultipartFile file) {
        String name = file.getOriginalFilename();
        return (name == null || name.isBlank()) ? "upload" : name;
    }
}

package com.campaignorganizer.media;

import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Public byte-serving endpoint (see ADR-0016). Addressed by unguessable UUID so
 * {@code <img src>} works without an Authorization header.
 */
@RestController
public class MediaContentController {

    private final MediaRepository media;
    private final MediaStorage storage;

    public MediaContentController(MediaRepository media, MediaStorage storage) {
        this.media = media;
        this.storage = storage;
    }

    @GetMapping("/api/media/{mediaId}/content")
    public ResponseEntity<Resource> content(@PathVariable UUID mediaId) {
        MediaAsset asset = media.findById(mediaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Media not found"));
        Resource resource = storage.load(asset.getStorageKey());
        if (!resource.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Media bytes missing");
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.getContentType()))
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30)).cachePublic())
                .body(resource);
    }
}

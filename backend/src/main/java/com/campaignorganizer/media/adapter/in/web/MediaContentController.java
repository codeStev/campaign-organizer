package com.campaignorganizer.media.adapter.in.web;

import com.campaignorganizer.media.application.port.in.LoadMediaContentUseCase;
import com.campaignorganizer.media.application.port.in.LoadMediaContentUseCase.MediaContent;
import java.time.Duration;
import java.util.UUID;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public byte-serving endpoint (ADR-0016). Addressed by unguessable UUID so
 * {@code <img src>} works without an Authorization header.
 */
@RestController
public class MediaContentController {

    private final LoadMediaContentUseCase loadUseCase;

    public MediaContentController(LoadMediaContentUseCase loadUseCase) {
        this.loadUseCase = loadUseCase;
    }

    @GetMapping("/api/media/{mediaId}/content")
    public ResponseEntity<Resource> content(@PathVariable UUID mediaId) {
        MediaContent media = loadUseCase.load(mediaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(media.contentType()))
                .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                .body(new ByteArrayResource(media.bytes()));
    }
}

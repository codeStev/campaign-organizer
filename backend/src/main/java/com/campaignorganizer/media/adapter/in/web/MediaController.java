package com.campaignorganizer.media.adapter.in.web;

import com.campaignorganizer.media.application.port.in.DeleteMediaUseCase;
import com.campaignorganizer.media.application.port.in.ListMediaUseCase;
import com.campaignorganizer.media.application.port.in.UploadMediaUseCase;
import com.campaignorganizer.media.application.port.in.UploadMediaUseCase.UploadMediaCommand;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
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

/** Thin web adapter for media upload/list/delete. */
@RestController
@RequestMapping("/api/worlds/{worldId}/media")
public class MediaController {

    private final UploadMediaUseCase uploadUseCase;
    private final ListMediaUseCase listUseCase;
    private final DeleteMediaUseCase deleteUseCase;
    private final MediaWebMapper mapper;

    public MediaController(UploadMediaUseCase uploadUseCase, ListMediaUseCase listUseCase,
                          DeleteMediaUseCase deleteUseCase, MediaWebMapper mapper) {
        this.uploadUseCase = uploadUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.mapper = mapper;
    }

    @GetMapping
    public List<MediaResponse> list(@PathVariable UUID worldId) {
        return listUseCase.list(worldId).stream().map(mapper::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<MediaResponse> upload(@PathVariable UUID worldId,
                                                @RequestParam("file") MultipartFile file) {
        UploadMediaCommand command = new UploadMediaCommand(
                worldId, safeFilename(file), file.getContentType(), readBytes(file));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mapper.toResponse(uploadUseCase.upload(command)));
    }

    @DeleteMapping("/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID worldId, @PathVariable UUID mediaId) {
        deleteUseCase.delete(worldId, mediaId);
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

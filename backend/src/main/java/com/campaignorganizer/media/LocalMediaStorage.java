package com.campaignorganizer.media;

import com.campaignorganizer.config.AppProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/** {@link MediaStorage} that writes one file per asset under {@code app.media.dir}. */
@Component
public class LocalMediaStorage implements MediaStorage {

    private final Path root;

    public LocalMediaStorage(AppProperties properties) {
        this.root = Path.of(properties.media().dir()).toAbsolutePath().normalize();
    }

    @Override
    public String store(byte[] data) {
        String key = UUID.randomUUID().toString();
        try {
            Files.createDirectories(root);
            Files.write(resolve(key), data);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store media", e);
        }
        return key;
    }

    @Override
    public Resource load(String key) {
        return new FileSystemResource(resolve(key));
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete media", e);
        }
    }

    /** Resolve a key safely under the root, rejecting traversal. */
    private Path resolve(String key) {
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        return path;
    }
}

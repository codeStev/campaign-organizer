package com.campaignorganizer.media.adapter.out.storage;

import com.campaignorganizer.config.AppProperties;
import com.campaignorganizer.media.application.port.out.MediaStoragePort;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** {@link MediaStoragePort} that writes one file per asset under {@code app.media.dir}. */
@Component
public class LocalMediaStorageAdapter implements MediaStoragePort {

    private final Path root;

    public LocalMediaStorageAdapter(AppProperties properties) {
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
    public Optional<byte[]> load(String key) {
        Path path = resolve(key);
        if (!Files.exists(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load media", e);
        }
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

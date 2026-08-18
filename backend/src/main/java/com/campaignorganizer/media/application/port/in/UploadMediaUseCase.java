package com.campaignorganizer.media.application.port.in;

import java.util.UUID;

public interface UploadMediaUseCase {

    MediaView upload(UploadMediaCommand command);

    record UploadMediaCommand(UUID worldId, String filename, String contentType, byte[] content) {
    }
}

package com.campaignorganizer.media.domain;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.campaignorganizer.shared.domain.ValidationException;
import org.junit.jupiter.api.Test;

/** Pure domain unit test of the upload rules. */
class MediaAssetTest {

    @Test
    void acceptsAnAllowedImageWithinTheSizeLimit() {
        assertThatCode(() -> MediaAsset.ensureUploadable("image/png", 1024))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsEmptyFile() {
        assertThatThrownBy(() -> MediaAsset.ensureUploadable("image/png", 0))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsOversizeFile() {
        assertThatThrownBy(() -> MediaAsset.ensureUploadable("image/png", MediaAsset.MAX_SIZE_BYTES + 1))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void rejectsNonImageType() {
        assertThatThrownBy(() -> MediaAsset.ensureUploadable("application/pdf", 100))
                .isInstanceOf(ValidationException.class);
    }
}

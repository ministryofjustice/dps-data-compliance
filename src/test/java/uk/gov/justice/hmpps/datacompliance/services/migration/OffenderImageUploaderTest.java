package uk.gov.justice.hmpps.datacompliance.services.migration;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.services.client.image.recognition.ImageRecognitionClient;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffenderImageUploaderTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("offender1");
    private static final long IMAGE_ID = 123L;
    private static final byte[] IMAGE_DATA = new byte[]{0x12};
    private static final OffenderImageMetadata IMAGE_METADATA = new OffenderImageMetadata(IMAGE_ID, "FACE");

    @Mock
    private Elite2ApiClient elite2ApiClient;

    @Mock
    private ImageRecognitionClient imageRecognitionClient;

    @Mock
    private OffenderImageUploadLogger logger;

    @Mock
    private RateLimiter rateLimiter;

    private OffenderImageUploader imageUploader;

    @BeforeEach
    void setUp() {
        imageUploader = new OffenderImageUploader(elite2ApiClient, imageRecognitionClient, logger, rateLimiter);
    }

    @Test
    void uploadOffenderImages() {

        when(elite2ApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(List.of(IMAGE_METADATA));

        when(elite2ApiClient.getImageData(IMAGE_ID)).thenReturn(Optional.of(IMAGE_DATA));

        when(imageRecognitionClient.uploadImageToCollection(IMAGE_DATA, OFFENDER_NUMBER, IMAGE_ID))
                .thenReturn(Optional.of("face1"));

        imageUploader.accept(OFFENDER_NUMBER);

        verify(rateLimiter).acquire();
        verify(logger).log(OFFENDER_NUMBER, IMAGE_METADATA, "face1");
    }

    @Test
    void uploadOffenderImagesHandlesNoImages() {

        when(elite2ApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(emptyList());

        imageUploader.accept(OFFENDER_NUMBER);

        verifyNoInteractions(imageRecognitionClient);
        verifyNoInteractions(logger);
    }

    @Test
    void uploadOffenderImagesHandlesMissingImageData() {

        when(elite2ApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(List.of(IMAGE_METADATA));

        when(elite2ApiClient.getImageData(IMAGE_ID)).thenReturn(Optional.empty());

        imageUploader.accept(OFFENDER_NUMBER);

        verifyNoInteractions(imageRecognitionClient);
        verifyNoInteractions(logger);
    }

    @Test
    void uploadMayResultInNoIndexedFaces() {

        when(elite2ApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(List.of(IMAGE_METADATA));

        when(elite2ApiClient.getImageData(IMAGE_ID)).thenReturn(Optional.of(IMAGE_DATA));

        when(imageRecognitionClient.uploadImageToCollection(IMAGE_DATA, OFFENDER_NUMBER, IMAGE_ID))
                .thenReturn(Optional.empty());

        imageUploader.accept(OFFENDER_NUMBER);

        verifyNoInteractions(logger);
    }

}
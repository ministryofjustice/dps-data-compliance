package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.FaceId;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.ImageRecognitionClient;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.mockito.Mockito.*;
import static uk.gov.justice.hmpps.datacompliance.client.image.recognition.IndexFacesError.FACE_NOT_FOUND;
import static uk.gov.justice.hmpps.datacompliance.utils.Result.error;
import static uk.gov.justice.hmpps.datacompliance.utils.Result.success;

@ExtendWith(MockitoExtension.class)
class OffenderImageUploaderTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final long IMAGE_ID = 123L;
    private static final byte[] IMAGE_DATA = new byte[]{0x12};
    private static final OffenderImageMetadata IMAGE_METADATA = new OffenderImageMetadata(IMAGE_ID, "FACE");

    @Mock
    private PrisonApiClient prisonApiClient;

    @Mock
    private ImageRecognitionClient imageRecognitionClient;

    @Mock
    private OffenderImageUploadLogger logger;

    @Mock
    private RateLimiter rateLimiter;

    private OffenderImageUploader imageUploader;

    @BeforeEach
    void setUp() {
        imageUploader = new OffenderImageUploader(prisonApiClient, imageRecognitionClient, logger, rateLimiter);
    }

    @Test
    void uploadOffenderImages() {

        when(prisonApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(List.of(IMAGE_METADATA));

        when(prisonApiClient.getImageData(IMAGE_ID)).thenReturn(Optional.of(IMAGE_DATA));

        when(imageRecognitionClient.uploadImageToCollection(IMAGE_DATA, OFFENDER_NUMBER, IMAGE_ID))
                .thenReturn(success(new FaceId("face1")));

        imageUploader.accept(OFFENDER_NUMBER);

        verify(rateLimiter).acquire();
        verify(logger).log(OFFENDER_NUMBER, IMAGE_METADATA, new FaceId("face1"));
    }

    @Test
    void uploadOffenderImagesHandlesNoImages() {

        when(prisonApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(emptyList());

        imageUploader.accept(OFFENDER_NUMBER);

        verifyNoInteractions(imageRecognitionClient);
        verifyNoInteractions(logger);
    }

    @Test
    void uploadOffenderImagesHandlesMissingImageData() {

        when(prisonApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(List.of(IMAGE_METADATA));

        when(prisonApiClient.getImageData(IMAGE_ID)).thenReturn(Optional.empty());

        imageUploader.accept(OFFENDER_NUMBER);

        verify(logger).logUploadError(OFFENDER_NUMBER, IMAGE_METADATA, "MISSING_IMAGE_DATA");
        verifyNoInteractions(imageRecognitionClient);
    }

    @Test
    void uploadMayResultInNoIndexedFaces() {

        when(prisonApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(List.of(IMAGE_METADATA));

        when(prisonApiClient.getImageData(IMAGE_ID)).thenReturn(Optional.of(IMAGE_DATA));

        when(imageRecognitionClient.uploadImageToCollection(IMAGE_DATA, OFFENDER_NUMBER, IMAGE_ID))
                .thenReturn(error(FACE_NOT_FOUND));

        imageUploader.accept(OFFENDER_NUMBER);

        verify(logger).logUploadError(OFFENDER_NUMBER, IMAGE_METADATA, "FACE_NOT_FOUND");
    }

}
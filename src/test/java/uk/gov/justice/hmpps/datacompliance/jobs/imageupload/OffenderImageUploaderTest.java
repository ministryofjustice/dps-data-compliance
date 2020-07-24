package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.IndexFacesError;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.OffenderImage;
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
    private static final FaceId FACE_ID = new FaceId("face1");
    private static final OffenderImageMetadata IMAGE_METADATA = new OffenderImageMetadata(IMAGE_ID, "FACE");
    private static final OffenderImage OFFENDER_IMAGE = OffenderImage.builder()
            .offenderNumber(OFFENDER_NUMBER)
            .imageId(IMAGE_ID)
            .imageData(new byte[]{0x12})
            .build();

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

        givenFaceImageExistsForOffender(true)
                .andImageAlreadyUploaded(false)
                .andImageDataExists(true)
                .andImageUploadsSuccessfully();

        imageUploader.accept(OFFENDER_NUMBER);

        verify(rateLimiter).acquire();
        verify(logger).log(OFFENDER_IMAGE, FACE_ID);
    }

    @Test
    void uploadOffenderImagesSkipsUploadIfAlreadyUploaded() {

        givenFaceImageExistsForOffender(true)
                .andImageAlreadyUploaded(true);

        imageUploader.accept(OFFENDER_NUMBER);

        verifyNoInteractions(imageRecognitionClient);
        verify(logger, never()).log(any(), any());
    }

    @Test
    void uploadOffenderImagesHandlesNoImages() {

        givenFaceImageExistsForOffender(false);

        imageUploader.accept(OFFENDER_NUMBER);

        verifyNoInteractions(imageRecognitionClient);
        verifyNoInteractions(logger);
    }

    @Test
    void uploadOffenderImagesHandlesMissingImageData() {

        givenFaceImageExistsForOffender(true)
                .andImageAlreadyUploaded(false)
                .andImageDataExists(false);

        imageUploader.accept(OFFENDER_NUMBER);

        verify(logger).logUploadError(OFFENDER_NUMBER, IMAGE_ID, "MISSING_IMAGE_DATA");
        verifyNoInteractions(imageRecognitionClient);
    }

    @Test
    void uploadMayResultInNoIndexedFaces() {

        givenFaceImageExistsForOffender(true)
                .andImageAlreadyUploaded(false)
                .andImageDataExists(true)
                .andImageUploadFailsWith(FACE_NOT_FOUND);

        imageUploader.accept(OFFENDER_NUMBER);

        verify(logger).logUploadError(OFFENDER_NUMBER, IMAGE_ID, "FACE_NOT_FOUND");
    }

    private OffenderImageUploaderTest givenFaceImageExistsForOffender(final boolean exists) {
        when(prisonApiClient.getOffenderFaceImagesFor(OFFENDER_NUMBER))
                .thenReturn(exists ? List.of(IMAGE_METADATA) : emptyList());
        return this;
    }

    private OffenderImageUploaderTest andImageAlreadyUploaded(final boolean uploaded) {
        when(logger.isAlreadyUploaded(OFFENDER_NUMBER, IMAGE_ID)).thenReturn(uploaded);
        return this;
    }

    private OffenderImageUploaderTest andImageDataExists(final boolean exists) {
        when(prisonApiClient.getImageData(OFFENDER_NUMBER, IMAGE_ID))
                .thenReturn(exists ? Optional.of(OFFENDER_IMAGE) : Optional.empty());
        return this;
    }

    private void andImageUploadsSuccessfully() {
        when(imageRecognitionClient.uploadImageToCollection(OFFENDER_IMAGE))
                .thenReturn(success(FACE_ID));
    }

    private void andImageUploadFailsWith(final IndexFacesError error) {
        when(imageRecognitionClient.uploadImageToCollection(OFFENDER_IMAGE))
                .thenReturn(error(error));
    }
}

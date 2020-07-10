package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import com.google.common.util.concurrent.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.ImageRecognitionClient;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.OffenderImage;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.jobs.imageupload.OffenderIterator.OffenderAction;

@Slf4j
@AllArgsConstructor
class OffenderImageUploader implements OffenderAction {

    private static final String MISSING_IMAGE_DATA = "MISSING_IMAGE_DATA";

    private final PrisonApiClient prisonApiClient;
    private final ImageRecognitionClient imageRecognitionClient;
    private final OffenderImageUploadLogger uploadLogger;
    private final RateLimiter rateLimiter;

    @Override
    public void accept(final OffenderNumber offenderNumber) {

        log.trace("Uploading image data for offender: '{}'", offenderNumber.getOffenderNumber());

        final var faceImages = prisonApiClient.getOffenderFaceImagesFor(offenderNumber);

        if (faceImages.isEmpty()) {
            log.trace("Offender: '{}' has no face images to upload", offenderNumber.getOffenderNumber());
            return;
        }

        faceImages.forEach(image -> getAndUploadImageData(image, offenderNumber));
    }

    long getUploadCount() {
        return uploadLogger.getUploadCount();
    }

    private void getAndUploadImageData(final OffenderImageMetadata imageMetadata, final OffenderNumber offenderNumber) {

        log.trace("Uploading image: '{}' for offender: '{}'", imageMetadata.getImageId(), offenderNumber.getOffenderNumber());

        final var image = prisonApiClient.getImageData(offenderNumber, imageMetadata.getImageId());

        image.ifPresentOrElse(

                this::uploadAndLogImage,

                () -> logMissingImageData(offenderNumber, imageMetadata.getImageId()));
    }

    private void uploadAndLogImage(final OffenderImage image) {

        rateLimiter.acquire();

        imageRecognitionClient.uploadImageToCollection(image)
                .handle(faceId -> uploadLogger.log(image, faceId),
                        error -> uploadLogger.logUploadError(image.getOffenderNumber(), image.getImageId(), error.getReason()));
    }

    private void logMissingImageData(final OffenderNumber offenderNumber, final long imageId) {

        log.warn("Image: '{}' for offender: '{}' has no image data", imageId, offenderNumber.getOffenderNumber());

        uploadLogger.logUploadError(offenderNumber, imageId, MISSING_IMAGE_DATA);
    }
}

package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import com.google.common.util.concurrent.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.PrisonApiClient;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.ImageRecognitionClient;
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

    private void getAndUploadImageData(final OffenderImageMetadata image, final OffenderNumber offenderNumber) {

        log.trace("Uploading image: '{}' for offender: '{}'", image.getImageId(), offenderNumber.getOffenderNumber());

        final var imageData = prisonApiClient.getImageData(image.getImageId());

        imageData.ifPresentOrElse(

                data -> uploadAndLogImage(image, offenderNumber, data),

                () -> logMissingImageData(image, offenderNumber));
    }

    private void uploadAndLogImage(final OffenderImageMetadata image,
                                   final OffenderNumber offenderNumber,
                                   final byte[] imageData) {

        rateLimiter.acquire();

        imageRecognitionClient.uploadImageToCollection(imageData, offenderNumber, image.getImageId())
                .handle(faceId -> uploadLogger.log(offenderNumber, image, faceId),
                        error -> uploadLogger.logUploadError(offenderNumber, image, error.getReason()));
    }

    private void logMissingImageData(final OffenderImageMetadata image, final OffenderNumber offenderNumber) {

        log.warn("Image: '{}' for offender: '{}' has no image data",
                image.getImageId(), offenderNumber.getOffenderNumber());

        uploadLogger.logUploadError(offenderNumber, image, MISSING_IMAGE_DATA);
    }
}

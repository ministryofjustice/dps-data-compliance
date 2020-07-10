package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.FaceId;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.OffenderImage;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Slf4j
@AllArgsConstructor
class OffenderImageUploadLogger {

    private final OffenderImageUploadRepository repository;
    private final ImageUploadBatch uploadBatch;
    private final TimeSource timeSource;
    private final AtomicLong uploadCount = new AtomicLong();

    void log(final OffenderImage image, final FaceId faceId) {

        final var offenderNumber = image.getOffenderNumber().getOffenderNumber();

        log.trace("Uploaded image: '{}' for offender: '{}'", image.getImageId(), offenderNumber);

        repository.findByOffenderNoAndImageId(offenderNumber, image.getImageId())
                .ifPresentOrElse(
                        logAlreadyExists(image.getImageId(), image.getOffenderNumber()),
                        save(image, faceId));
    }

    void logUploadError(final OffenderNumber offenderNumber, final long imageId, final String reason) {

        log.debug("Upload for image: '{}' and offender: '{}' failed due to: '{}'",
                imageId, offenderNumber.getOffenderNumber(), reason);

        repository.findByOffenderNoAndImageId(offenderNumber.getOffenderNumber(), imageId)
                .ifPresentOrElse(
                        logAlreadyExists(imageId, offenderNumber),
                        saveUploadError(offenderNumber, imageId, reason));
    }

    long getUploadCount() {
        return uploadCount.get();
    }

    private Consumer<OffenderImageUpload> logAlreadyExists(final long imageId,
                                                           final OffenderNumber offenderNumber) {
        return existingUpload -> log.warn("Image: '{}' for offender: '{}' has already been uploaded.",
                imageId, offenderNumber.getOffenderNumber());
    }

    private Runnable saveUploadError(final OffenderNumber offenderNumber, final long imageId, final String reason) {
        return () -> repository.save(
                offenderImageUploadBuilder(offenderNumber, imageId)
                        .uploadErrorReason(reason)
                        .build());
    }

    private Runnable save(final OffenderImage image, final FaceId faceId) {
        return () -> {
            repository.save(
                    offenderImageUploadBuilder(image.getOffenderNumber(), image.getImageId())
                            .faceId(faceId.getFaceId())
                            .build());
            uploadCount.incrementAndGet();
        };
    }

    private OffenderImageUpload.OffenderImageUploadBuilder offenderImageUploadBuilder(final OffenderNumber offenderNumber,
                                                                                      final long imageId) {
        return OffenderImageUpload.builder()
                .imageUploadBatch(uploadBatch)
                .uploadDateTime(timeSource.nowAsLocalDateTime())
                .offenderNo(offenderNumber.getOffenderNumber())
                .imageId(imageId);
    }
}

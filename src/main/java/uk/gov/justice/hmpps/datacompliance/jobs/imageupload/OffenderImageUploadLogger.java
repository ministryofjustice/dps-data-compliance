package uk.gov.justice.hmpps.datacompliance.jobs.imageupload;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.gov.justice.hmpps.datacompliance.client.prisonapi.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.OffenderImageUpload;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.client.image.recognition.FaceId;
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

    void log(final OffenderNumber offenderNumber, final OffenderImageMetadata image, final FaceId faceId) {

        log.trace("Uploaded image: '{}' for offender: '{}'", image.getImageId(), offenderNumber.getOffenderNumber());

        repository.findByOffenderNoAndImageId(offenderNumber.getOffenderNumber(), image.getImageId())
                .ifPresentOrElse(
                        logAlreadyExists(image, offenderNumber),
                        save(offenderNumber, image, faceId));
    }

    void logUploadError(final OffenderNumber offenderNumber, final OffenderImageMetadata image, final String reason) {

        log.debug("Upload for image: '{}' and offender: '{}' failed due to: '{}'",
                image.getImageId(), offenderNumber.getOffenderNumber(), reason);

        repository.findByOffenderNoAndImageId(offenderNumber.getOffenderNumber(), image.getImageId())
                .ifPresentOrElse(
                        logAlreadyExists(image, offenderNumber),
                        saveUploadError(offenderNumber, image, reason));
    }

    long getUploadCount() {
        return uploadCount.get();
    }

    private Consumer<OffenderImageUpload> logAlreadyExists(final OffenderImageMetadata image,
                                                           final OffenderNumber offenderNumber) {
        return existingUpload -> log.warn("Image: '{}' for offender: '{}' has already been uploaded.",
                image.getImageId(), offenderNumber.getOffenderNumber());
    }

    private Runnable saveUploadError(final OffenderNumber offenderNumber, final OffenderImageMetadata image, final String reason) {
        return () -> repository.save(
                offenderImageUploadBuilder(offenderNumber, image)
                        .uploadErrorReason(reason)
                        .build());
    }

    private Runnable save(final OffenderNumber offenderNumber, final OffenderImageMetadata image, final FaceId faceId) {
        return () -> {
            repository.save(
                    offenderImageUploadBuilder(offenderNumber, image)
                            .faceId(faceId.getFaceId())
                            .build());
            uploadCount.incrementAndGet();
        };
    }

    private OffenderImageUpload.OffenderImageUploadBuilder offenderImageUploadBuilder(final OffenderNumber offenderNumber,
                                                                                      final OffenderImageMetadata image) {
        return OffenderImageUpload.builder()
                .imageUploadBatch(uploadBatch)
                .uploadDateTime(timeSource.nowAsLocalDateTime())
                .offenderNo(offenderNumber.getOffenderNumber())
                .imageId(image.getImageId());
    }
}

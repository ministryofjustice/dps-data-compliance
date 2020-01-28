package uk.gov.justice.hmpps.datacompliance.services.migration;

import lombok.AllArgsConstructor;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderImageMetadata;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.model.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.model.OffenderImageUpload;
import uk.gov.justice.hmpps.datacompliance.repository.OffenderImageUploadRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.concurrent.atomic.AtomicLong;

@AllArgsConstructor
class OffenderImageUploadLogger {

    private final OffenderImageUploadRepository repository;
    private final ImageUploadBatch uploadBatch;
    private final TimeSource timeSource;
    private final AtomicLong uploadCount = new AtomicLong();

    void log(final OffenderNumber offenderNumber, final OffenderImageMetadata image, final String faceId) {
        repository.save(offenderImageUpload(offenderNumber, image, faceId));
        uploadCount.incrementAndGet();
    }

    long getUploadCount() {
        return uploadCount.get();
    }

    private OffenderImageUpload offenderImageUpload(final OffenderNumber offenderNumber,
                                                    final OffenderImageMetadata image, final String faceId) {
        return OffenderImageUpload.builder()
                .imageUploadBatch(uploadBatch)
                .uploadDateTime(timeSource.nowAsLocalDateTime())
                .offenderNo(offenderNumber.getOffenderNumber())
                .imageId(image.getImageId())
                .faceId(faceId)
                .build();
    }
}

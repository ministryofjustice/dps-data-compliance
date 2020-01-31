package uk.gov.justice.hmpps.datacompliance.services.migration;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.model.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.ImageUploadBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

@Slf4j
@Service
@AllArgsConstructor
@ConditionalOnProperty(name = "image.recognition.migration.cron")
class OffenderImageMigration {

    private final OffenderIterator offenderIterator;
    private final OffenderImageUploaderFactory uploaderFactory;
    private final ImageUploadBatchRepository repository;
    private final TimeSource timeSource;

    void run() {

        log.info("Running offender image migration");

        final var batch = repository.save(newUploadBatch());
        final var imageUploader = uploaderFactory.generateUploaderFor(batch);

        offenderIterator.applyForAll(imageUploader);

        batch.setUploadEndDateTime(timeSource.nowAsLocalDateTime());
        batch.setUploadCount(imageUploader.getUploadCount());
        repository.save(batch);

        log.info("Offender image migration complete, {} faces have been indexed", imageUploader.getUploadCount());
    }

    private ImageUploadBatch newUploadBatch() {
        return ImageUploadBatch.builder()
                .uploadStartDateTime(timeSource.nowAsLocalDateTime())
                .build();
    }
}

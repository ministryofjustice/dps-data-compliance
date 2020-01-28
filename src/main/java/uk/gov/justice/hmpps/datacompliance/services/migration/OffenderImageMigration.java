package uk.gov.justice.hmpps.datacompliance.services.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.model.ImageUploadBatch;
import uk.gov.justice.hmpps.datacompliance.repository.ImageUploadBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import static com.microsoft.applicationinsights.agent.shadow.com.google.common.collect.Iterables.size;

@Slf4j
@Service
@ConditionalOnProperty(name = "image.recognition.migration.cron")
class OffenderImageMigration {

    private final OffenderIterator offenderIterator;
    private final OffenderImageUploaderFactory uploaderFactory;
    private final ImageUploadBatchRepository repository;
    private final TimeSource timeSource;

    OffenderImageMigration(final OffenderIterator offenderIterator,
                           final OffenderImageUploaderFactory uploaderFactory,
                           final ImageUploadBatchRepository repository,
                           final TimeSource timeSource,
                           @Value("${image.recognition.migration.cron}") final String migrationCron) {

        log.info("Configured to run offender image recognition migration with schedule: '{}'", migrationCron);

        this.offenderIterator = offenderIterator;
        this.uploaderFactory = uploaderFactory;
        this.repository = repository;
        this.timeSource = timeSource;
    }

    @Scheduled(cron = "${image.recognition.migration.cron}")
    void run() {

        log.info("Running offender image migration");

        if (size(repository.findAll()) != 0) {
            log.warn("For first proof of concept, we only need this to run once");
            return;
        }

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

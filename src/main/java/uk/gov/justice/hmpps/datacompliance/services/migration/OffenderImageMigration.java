package uk.gov.justice.hmpps.datacompliance.services.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "image.recognition.migration.cron")
public class OffenderImageMigration {

    private final OffenderIterator offenderIterator;
    private final OffenderImageUploader offenderImageUploader;

    public OffenderImageMigration(final OffenderIterator offenderIterator,
                                  final OffenderImageUploader offenderImageUploader,
                                  @Value("${image.recognition.migration.cron}") final String migrationCron) {

        log.info("Configured to run offender image recognition migration with schedule: '{}'", migrationCron);

        this.offenderIterator = offenderIterator;
        this.offenderImageUploader = offenderImageUploader;
    }

    @Scheduled(cron = "${image.recognition.migration.cron}")
    public void run() {

            log.info("Running offender image migration");

            offenderIterator.applyForAll(offenderImageUploader);

            log.info("Offender image migration complete");

    }
}

package uk.gov.justice.hmpps.datacompliance.jobs.offenderdeletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.config.OffenderDeletionConfig;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import javax.transaction.Transactional;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
@ConditionalOnProperty(name = "offender.deletion.cron")
class OffenderDeletion {

    private final TimeSource timeSource;
    private final OffenderDeletionConfig config;
    private final OffenderDeletionBatchRepository repository;
    private final Elite2ApiClient elite2ApiClient;

    void run() {

        log.info("Running offender deletion");

        final var windowStart = getLastBatch().map(OffenderDeletionBatch::getWindowEndDateTime)
                .orElse(config.getInitialWindowStart());
        final var windowEnd = windowStart.plus(config.getWindowLength());

        log.info("Deleting offenders due for deletion between: {} and {}", windowStart, windowEnd);

        final var batch = repository.save(OffenderDeletionBatch.builder()
                .requestDateTime(timeSource.nowAsLocalDateTime())
                .windowStartDateTime(windowStart)
                .windowEndDateTime(windowEnd)
                .build());

        elite2ApiClient.requestPendingDeletions(windowStart, windowEnd, batch.getBatchId());

        log.info("Offender deletion request complete");
    }

    private Optional<OffenderDeletionBatch> getLastBatch() {
        return repository.findFirstByOrderByRequestDateTimeDesc();
    }
}

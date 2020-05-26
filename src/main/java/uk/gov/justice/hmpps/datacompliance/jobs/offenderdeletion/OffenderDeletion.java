package uk.gov.justice.hmpps.datacompliance.jobs.offenderdeletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.elite2api.Elite2ApiClient;
import uk.gov.justice.hmpps.datacompliance.client.elite2api.dto.PendingDeletionsRequest;
import uk.gov.justice.hmpps.datacompliance.config.OffenderDeletionConfig;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

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

        final var newBatch = persistNewBatch();
        final var request = PendingDeletionsRequest.builder()
                .dueForDeletionWindowStart(newBatch.getWindowStartDateTime())
                .dueForDeletionWindowEnd(newBatch.getWindowEndDateTime())
                .batchId(newBatch.getBatchId());

        config.getReferralLimit().ifPresent(request::limit);

        elite2ApiClient.requestPendingDeletions(request.build());

        log.info("Offender deletion request complete");
    }

    private OffenderDeletionBatch persistNewBatch() {

        final var windowStart = windowStart();
        final var windowEnd = windowStart.plus(config.getWindowLength());
        validateWindow(windowStart, windowEnd);

        log.info("Deleting offenders due for deletion between: {} and {}", windowStart, windowEnd);

        return repository.save(OffenderDeletionBatch.builder()
                .requestDateTime(timeSource.nowAsLocalDateTime())
                .windowStartDateTime(windowStart)
                .windowEndDateTime(windowEnd)
                .build());
    }

    private LocalDateTime windowStart() {
        return getLastBatch()
                .map(this::getNextWindowStart)
                .orElse(config.getInitialWindowStart());
    }

    private Optional<OffenderDeletionBatch> getLastBatch() {
        return repository.findFirstByOrderByRequestDateTimeDesc();
    }

    private LocalDateTime getNextWindowStart(final OffenderDeletionBatch lastBatch) {

        checkNotNull(lastBatch.getReferralCompletionDateTime(),
                "Previous referral (%s) did not complete", lastBatch.getBatchId());

        return lastBatch.hasRemainingInWindow() ? lastBatch.getWindowStartDateTime() : lastBatch.getWindowEndDateTime();
    }

    private void validateWindow(final LocalDateTime windowStart, final LocalDateTime windowEnd) {
        checkArgument(windowStart.isBefore(timeSource.nowAsLocalDateTime()),
                "Deletion due date cannot be in the future, window start date is not valid: %s", windowStart);
        checkArgument(windowEnd.isBefore(timeSource.nowAsLocalDateTime()),
                "Deletion due date cannot be in the future, window end date is not valid: %s", windowEnd);
        checkState(windowStart.isBefore(windowEnd),
                "Deletion due window dates are illogical: '%s' > '%s'", windowStart, windowEnd);
    }
}

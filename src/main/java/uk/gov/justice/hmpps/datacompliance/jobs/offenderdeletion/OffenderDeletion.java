package uk.gov.justice.hmpps.datacompliance.jobs.offenderdeletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.config.OffenderDeletionConfig;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionReferralRequest;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.SCHEDULED;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
@ConditionalOnProperty(name = "offender.deletion.cron")
public class OffenderDeletion {

    private final TimeSource timeSource;
    private final OffenderDeletionConfig config;
    private final OffenderDeletionBatchRepository repository;
    private final DataComplianceEventPusher eventPusher;
    private final DataComplianceProperties properties;
    private final OffenderDeletionReferralRepository offenderDeletionReferralRepository;

    void run() {
        requestBatchReferral();
        deletePreviouslyIdentifiedOffenderData();
    }

    public void requestBatchReferral() {
        log.info("Running a new offender deletion batch");
        final var newBatch = persistNewBatch();

        final var request = OffenderDeletionReferralRequest.builder()
            .batchId(newBatch.getBatchId())
            .dueForDeletionWindowStart(newBatch.getWindowStartDateTime().toLocalDate())
            .dueForDeletionWindowEnd(newBatch.getWindowEndDateTime().toLocalDate());

        config.getReferralLimit().ifPresent(request::limit);

        eventPusher.requestReferral(request.build());

        log.info("Offender deletion request complete");
    }

    public void deletePreviouslyIdentifiedOffenderData() {
        if (properties.isDeletionGrantEnabled() && properties.isReviewRequired()) {
            log.info("Running an offender deletion review");

            final var deletionReviewDelay = config.getReviewDuration();

            final var deleteAfter = timeSource.nowAsLocalDateTime().minus(deletionReviewDelay);
            final var prospectiveDeletionReferrals = offenderDeletionReferralRepository.findByReferralResolutionStatus(ResolutionStatus.PROVISIONAL_DELETION_GRANTED.name(), true, deleteAfter, config.getDeletionLimit());

            prospectiveDeletionReferrals.forEach(referral -> eventPusher.requestProvisionalDeletionReferral(referral.getOffenderNumber(), referral.getReferralId()));
        }
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
            .batchType(SCHEDULED)
            .build());
    }

    private LocalDateTime windowStart() {
        return getLastScheduledBatch()
            .map(this::getNextWindowStart)
            .orElse(config.getInitialWindowStart());
    }

    private Optional<OffenderDeletionBatch> getLastScheduledBatch() {
        return repository.findFirstByBatchTypeOrderByRequestDateTimeDesc(SCHEDULED);
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

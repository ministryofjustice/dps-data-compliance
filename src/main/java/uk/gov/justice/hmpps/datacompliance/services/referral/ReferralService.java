package uk.gov.justice.hmpps.datacompliance.services.referral;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderToCheck;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralComplete;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderIds;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class ReferralService {

    private final TimeSource timeSource;
    private final OffenderDeletionBatchRepository batchRepository;
    private final RetentionService retentionService;
    private final ReferralResolutionService referralResolutionService;

    public void handlePendingDeletionReferral(final OffenderPendingDeletion event) {

        final var referral = createReferral(event);

        final var retentionChecks = retentionService.conductRetentionChecks(
                OffenderToCheck.builder()
                        .offenderNumber(new OffenderNumber(event.getOffenderIdDisplay()))
                        .offenceCodes(event.getOffenceCodes())
                        .build());

        referralResolutionService.processReferral(referral, retentionChecks);
    }

    public void handleReferralComplete(final OffenderPendingDeletionReferralComplete event) {

        log.info("All offenders pending deletion in batch: '{}' have been added to the queue", event.getBatchId());

        final var batch = batchRepository.findById(event.getBatchId())
                .orElseThrow(illegalState("Cannot find batch with id: '%s'", event.getBatchId()));

        batch.setReferralCompletionDateTime(timeSource.nowAsLocalDateTime());
        batch.setRemainingInWindow((int) (event.getTotalInWindow() - event.getNumberReferred()));

        batchRepository.save(batch);
    }

    private OffenderDeletionReferral createReferral(final OffenderPendingDeletion event) {

        final var batch = batchRepository.findById(event.getBatchId())
                .orElseThrow(illegalState("Cannot find deletion batch with id: '%s'", event.getBatchId()));

        final var referral = OffenderDeletionReferral.builder()
                .offenderDeletionBatch(batch)
                .receivedDateTime(timeSource.nowAsLocalDateTime())
                .offenderNo(event.getOffenderIdDisplay())
                .firstName(event.getFirstName())
                .middleName(event.getMiddleName())
                .lastName(event.getLastName())
                .birthDate(event.getBirthDate())
                .build();

        event.getOffenders().forEach(offender -> {

            final var offenderIds = ReferredOffenderIds.builder().offenderId(offender.getOffenderId());

            offender.getOffenderBookings().forEach(booking -> offenderIds.offenderBookId(booking.getOffenderBookId()));

            referral.addReferredOffenderIds(offenderIds.build());
        });

        return referral;
    }
}

package uk.gov.justice.hmpps.datacompliance.services.referral;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sns.OffenderDeletionCompleteEventPusher;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent.Booking;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent.OffenderWithBookings;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderBooking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.services.retention.ActionableRetentionCheck;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.groupingBy;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.RETAINED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class DeletionReferralService {

    private final TimeSource timeSource;
    private final OffenderDeletionBatchRepository batchRepository;
    private final OffenderDeletionReferralRepository referralRepository;
    private final DataComplianceEventPusher deletionGrantedEventPusher;
    private final OffenderDeletionCompleteEventPusher deletionCompleteEventPusher;
    private final RetentionService retentionService;

    public void handlePendingDeletion(final OffenderPendingDeletionEvent event) {

        final var referral = createReferral(event);

        final var retentionChecks = retentionService.conductRetentionChecks(
                new OffenderNumber(event.getOffenderIdDisplay()));

        processRetentionChecks(referral, retentionChecks);
    }

    public void handleReferralComplete(final OffenderPendingDeletionReferralCompleteEvent event) {

        log.info("All offenders pending deletion in batch: '{}' have been added to the queue", event.getBatchId());

        final var batch = batchRepository.findById(event.getBatchId())
                .orElseThrow(illegalState("Cannot find batch with id: '%s'", event.getBatchId()));

        batch.setReferralCompletionDateTime(timeSource.nowAsLocalDateTime());

        batchRepository.save(batch);
    }

    public void handleDeletionComplete(final OffenderDeletionCompleteEvent event) {

        final var referral = referralRepository.findById(event.getReferralId())
                .orElseThrow(illegalState("Cannot retrieve referral record for id: '%s'", event.getReferralId()));

        checkState(Objects.equals(event.getOffenderIdDisplay(), referral.getOffenderNo()),
                "Offender number '%s' of referral '%s' does not match '%s'",
                referral.getOffenderNo(), referral.getReferralId(), event.getOffenderIdDisplay());

        recordDeletionCompletion(referral);
        publishDeletionCompleteEvent(referral);
    }

    private void processRetentionChecks(final OffenderDeletionReferral referral,
                                        final List<ActionableRetentionCheck> retentionChecks) {

        checkState(!retentionChecks.isEmpty(),
                "No retention checks have been conducted for offender: '%s'", referral.getOffenderNo());

        if (anyPending(retentionChecks)) {
            persistRetentionChecks(referral, retentionChecks, PENDING);
            retentionChecks.forEach(ActionableRetentionCheck::triggerPendingCheck);
            return;
        }

        if (canGrantDeletion(retentionChecks)) {
            persistRetentionChecks(referral, retentionChecks, DELETION_GRANTED);
            grantDeletion(referral);
            return;
        }

        persistRetentionChecks(referral, retentionChecks, RETAINED);
    }

    private boolean anyPending(final List<ActionableRetentionCheck> retentionChecks) {
        return retentionChecks.stream().anyMatch(ActionableRetentionCheck::isPending);
    }

    private boolean canGrantDeletion(final List<ActionableRetentionCheck> retentionChecks) {
        return retentionChecks.stream().allMatch(check -> check.isStatus(RETENTION_NOT_REQUIRED));
    }

    private void persistRetentionChecks(final OffenderDeletionReferral referral,
                                        final List<ActionableRetentionCheck> retentionChecks,
                                        final ResolutionStatus resolutionStatus) {

        log.info("Offender referral '{}' has resolution status : '{}'", referral.getOffenderNo(), resolutionStatus);

        final var resolution = ReferralResolution.builder()
                .resolutionDateTime(timeSource.nowAsLocalDateTime())
                .resolutionStatus(resolutionStatus)
                .build();

        retentionChecks.stream()
                .map(ActionableRetentionCheck::getRetentionCheck)
                .forEach(resolution::addRetentionCheck);

        referral.setReferralResolution(resolution);
        referralRepository.save(referral);
    }

    private void grantDeletion(final OffenderDeletionReferral referral) {
        deletionGrantedEventPusher.grantDeletion(new OffenderNumber(referral.getOffenderNo()), referral.getReferralId());
    }

    private void recordDeletionCompletion(final OffenderDeletionReferral referral) {

        log.info("Updating destruction log with deletion confirmation for: '{}'", referral.getOffenderNo());

        final var referralResolution = referral.getReferralResolution()
                .filter(resolution -> resolution.isType(DELETION_GRANTED))
                .orElseThrow(illegalState("Referral '%s' does not have expected resolution type", referral.getReferralId()));

        referralResolution.setResolutionDateTime(timeSource.nowAsLocalDateTime());
        referralResolution.setResolutionStatus(DELETED);

        referralRepository.save(referral);
    }

    private OffenderDeletionReferral createReferral(final OffenderPendingDeletionEvent event) {

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

        event.getOffenders().forEach(offender ->
                offender.getOffenderBookings().forEach(booking ->
                        referral.addReferredOffenderBooking(ReferredOffenderBooking.builder()
                                .offenderId(offender.getOffenderId())
                                .offenderBookId(booking.getOffenderBookId())
                                .build())));

        return referral;
    }

    private void publishDeletionCompleteEvent(final OffenderDeletionReferral deletionCompletion) {

        log.info("Publishing deletion complete event for: '{}'", deletionCompletion.getOffenderNo());

        final var deletionCompleteEvent =
                uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent.builder()
                        .offenderIdDisplay(deletionCompletion.getOffenderNo());

        deletionCompletion.getOffenderBookings().stream()
                .collect(groupingBy(ReferredOffenderBooking::getOffenderId))
                .forEach((offenderId, bookings) -> {
                    final var offenderWithBookings =  OffenderWithBookings.builder().offenderId(offenderId);
                    bookings.forEach(booking -> offenderWithBookings.booking(new Booking(booking.getOffenderBookId())));
                    deletionCompleteEvent.offender(offenderWithBookings.build());
                });

        deletionCompleteEventPusher.sendEvent(deletionCompleteEvent.build());
    }
}

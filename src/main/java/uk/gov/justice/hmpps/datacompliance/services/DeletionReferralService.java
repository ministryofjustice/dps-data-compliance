package uk.gov.justice.hmpps.datacompliance.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted.OffenderDeletionGrantedEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderBooking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionType.DELETED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionType.DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class DeletionReferralService {

    private final TimeSource timeSource;
    private final OffenderDeletionReferralRepository repository;
    private final OffenderDeletionGrantedEventPusher eventPusher;
    private final RetentionService retentionService;

    public void handlePendingDeletion(final OffenderPendingDeletionEvent event) {

        final var referral = createOffenderDeletionReferral(event);

        if (retentionService.isOffenderEligibleForDeletion(new OffenderNumber(event.getOffenderIdDisplay()))) {
            log.info("No reason found to retain offender record '{}', granting deletion", event.getOffenderIdDisplay());
            grantDeletion(referral);
            return;
        }

        // TODO GDPR-63 Persist retention reasons
        log.info("Offender record '{}' has been marked for retention ", event.getOffenderIdDisplay());
    }

    public void handleReferralComplete(final OffenderPendingDeletionReferralCompleteEvent event) {
        // TODO GDPR-99 Track referral requests and add health check
    }

    public void handleDeletionComplete(final OffenderDeletionCompleteEvent event) {

        updateDestructionLog(event);

        // TODO GDPR-68 Publish SNS event
    }

    private void grantDeletion(final OffenderDeletionReferral referral) {

        referral.setReferralResolution(ReferralResolution.builder()
                .resolutionDateTime(timeSource.nowAsLocalDateTime())
                .resolutionType(DELETION_GRANTED)
                .build());

        repository.save(referral);
        eventPusher.grantDeletion(referral.getOffenderNo());
    }

    private void updateDestructionLog(final OffenderDeletionCompleteEvent event) {

        final var referral = repository.findById(event.getReferralId())
                .orElseThrow(illegalState("Cannot retrieve referral record for id: '%s'", event.getReferralId()));

        checkState(Objects.equals(referral.getOffenderNo(), event.getOffenderIdDisplay()),
                "Offender number '%s' of referral '%s' does not match '%s'",
                referral.getOffenderNo(), referral.getReferralId(), event.getOffenderIdDisplay());

        final var referralResolution = referral.getReferralResolution()
                .filter(resolution -> resolution.isType(DELETION_GRANTED))
                .orElseThrow(illegalState("Referral '%s' does not have expected resolution type", referral.getReferralId()));

        referralResolution.setResolutionDateTime(timeSource.nowAsLocalDateTime());
        referralResolution.setResolutionType(DELETED);

        repository.save(referral);
    }

    private OffenderDeletionReferral createOffenderDeletionReferral(final OffenderPendingDeletionEvent event) {

        final var referral = OffenderDeletionReferral.builder()
                .receivedDateTime(timeSource.nowAsLocalDateTime())
                .offenderNo(event.getOffenderIdDisplay())
                .firstName(event.getFirstName())
                .middleName(event.getMiddleName())
                .lastName(event.getLastName())
                .birthDate(event.getBirthDate())
                .build();

        event.getOffenders().forEach(offender ->
                offender.getBookings().forEach(booking ->
                        referral.addReferredOffenderBooking(ReferredOffenderBooking.builder()
                                .offenderId(offender.getOffenderId())
                                .offenderBookId(booking.getOffenderBookId())
                                .build())));

        return referral;
    }
}

package uk.gov.justice.hmpps.datacompliance.services;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted.OffenderDeletionGrantedEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ReferredOffenderBooking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

@Slf4j
@Service
@AllArgsConstructor
public class DeletionReferralService {

    private final TimeSource timeSource;
    private final OffenderDeletionReferralRepository repository;
    private final OffenderDeletionGrantedEventPusher eventPusher;
    private final RetentionService retentionService;

    public void handlePendingDeletion(final OffenderPendingDeletionEvent event) {

        storeOffenderDeletionReferral(event);

        if (retentionService.isOffenderEligibleForDeletion(new OffenderNumber(event.getOffenderIdDisplay()))) {
            log.info("No reason found to retain offender record '{}', granting deletion", event.getOffenderIdDisplay());
            eventPusher.grantDeletion(event.getOffenderIdDisplay());
        }

        log.info("Offender record '{}' has been marked for retention ", event.getOffenderIdDisplay());
    }

    public void handleReferralComplete(final OffenderPendingDeletionReferralCompleteEvent event) {
        // TODO GDPR-99 Track referral requests and add health check
    }

    public void handleDeletionComplete(final OffenderDeletionCompleteEvent event) {
        // TODO GDPR-72 Update destruction log
        // TODO GDPR-68 Publish SNS event
    }

    private void storeOffenderDeletionReferral(final OffenderPendingDeletionEvent event) {

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

        repository.save(referral);
    }
}

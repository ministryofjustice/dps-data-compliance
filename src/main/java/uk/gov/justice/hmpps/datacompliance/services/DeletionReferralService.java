package uk.gov.justice.hmpps.datacompliance.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted.OffenderDeletionGrantedEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ReferredOffenderBooking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

@Service
@AllArgsConstructor
public class DeletionReferralService {

    private TimeSource timeSource;
    private OffenderDeletionReferralRepository offenderDeletionReferralRepository;
    private OffenderDeletionGrantedEventPusher offenderDeletionGrantedEventPusher;

    public void handlePendingDeletion(final OffenderPendingDeletionEvent event) {
        storeOffenderDeletionReferral(event);

        // TODO GDPR-51 Complete retention checks on offender record

        offenderDeletionGrantedEventPusher.sendEvent(event.getOffenderIdDisplay());
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

        offenderDeletionReferralRepository.save(referral);
    }
}

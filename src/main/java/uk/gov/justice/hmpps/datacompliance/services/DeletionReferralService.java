package uk.gov.justice.hmpps.datacompliance.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.events.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.ReferredOffenderBooking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

@Service
@AllArgsConstructor
public class DeletionReferralService {

    private TimeSource timeSource;
    private OffenderDeletionReferralRepository offenderDeletionReferralRepository;

    public void storeOffenderDeletionReferral(final OffenderPendingDeletionEvent event) {

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

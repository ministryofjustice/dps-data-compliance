package uk.gov.justice.hmpps.datacompliance.services.referral;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.completed.OffenderDeletionCompleteEventPusher;
import uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted.OffenderDeletionGrantedEventPusher;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent.Booking;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent.OffenderWithBookings;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderBooking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReason;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.groupingBy;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionType.DELETED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionType.DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionType.RETAINED;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class DeletionReferralService {

    private final TimeSource timeSource;
    private final OffenderDeletionReferralRepository repository;
    private final OffenderDeletionGrantedEventPusher deletionGrantedEventPusher;
    private final OffenderDeletionCompleteEventPusher deletionCompleteEventPusher;
    private final RetentionService retentionService;

    public void handlePendingDeletion(final OffenderPendingDeletionEvent event) {

        final var referral = createOffenderDeletionReferral(event);

        retentionService.findRetentionReason(new OffenderNumber(event.getOffenderIdDisplay()))
                .ifPresentOrElse(
                        retentionReason -> markForRetention(referral, retentionReason),
                        () -> grantDeletion(referral));
    }

    public void handleReferralComplete(final OffenderPendingDeletionReferralCompleteEvent event) {
        // TODO GDPR-99 Track referral requests and add health check
    }

    public void handleDeletionComplete(final OffenderDeletionCompleteEvent event) {

        final var referral = repository.findById(event.getReferralId())
                .orElseThrow(illegalState("Cannot retrieve referral record for id: '%s'", event.getReferralId()));

        checkState(Objects.equals(event.getOffenderIdDisplay(), referral.getOffenderNo()),
                "Offender number '%s' of referral '%s' does not match '%s'",
                referral.getOffenderNo(), referral.getReferralId(), event.getOffenderIdDisplay());

        recordDeletionCompletion(referral);
        publishDeletionCompleteEvent(referral);
    }

    private void markForRetention(final OffenderDeletionReferral referral, final RetentionReason retentionReason) {

        log.info("Offender record '{}' has been marked for retention ", referral.getOffenderNo());

        final var resolution = ReferralResolution.builder()
                .resolutionDateTime(timeSource.nowAsLocalDateTime())
                .resolutionType(RETAINED)
                .build();

        resolution.setRetentionReason(retentionReason);
        referral.setReferralResolution(resolution);
        repository.save(referral);
    }

    private void grantDeletion(final OffenderDeletionReferral referral) {

        log.info("No reason found to retain offender record '{}', granting deletion", referral.getOffenderNo());

        referral.setReferralResolution(ReferralResolution.builder()
                .resolutionDateTime(timeSource.nowAsLocalDateTime())
                .resolutionType(DELETION_GRANTED)
                .build());

        repository.save(referral);
        deletionGrantedEventPusher.grantDeletion(new OffenderNumber(referral.getOffenderNo()), referral.getReferralId());
    }

    private OffenderDeletionReferral recordDeletionCompletion(final OffenderDeletionReferral referral) {

        log.info("Updating destruction log with deletion confirmation for: '{}'", referral.getOffenderNo());

        final var referralResolution = referral.getReferralResolution()
                .filter(resolution -> resolution.isType(DELETION_GRANTED))
                .orElseThrow(illegalState("Referral '%s' does not have expected resolution type", referral.getReferralId()));

        referralResolution.setResolutionDateTime(timeSource.nowAsLocalDateTime());
        referralResolution.setResolutionType(DELETED);

        return repository.save(referral);
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
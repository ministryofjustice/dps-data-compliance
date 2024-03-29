package uk.gov.justice.hmpps.datacompliance.services.deletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionGrant;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionComplete;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionComplete.Booking;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionComplete.OffenderWithBookings;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class DeletionService {

    private final TimeSource timeSource;
    private final OffenderDeletionReferralRepository referralRepository;
    private final DataComplianceEventPusher deletionGrantedEventPusher;
    private final DataComplianceProperties properties;
    private final ImageDuplicationDetectionService imageDuplicationDetectionService;

    public void grantDeletion(final OffenderDeletionReferral referral) {

        if (!properties.isDeletionGrantEnabled()) {
            log.info("Deletion grant is disabled for: '{}'", referral.getOffenderNo());
            return;
        }

        log.info("Granting deletion of offender: '{}'", referral.getOffenderNo());

        deletionGrantedEventPusher.grantDeletion(OffenderDeletionGrant.builder()
            .offenderNumber(referral.getOffenderNumber())
            .referralId(referral.getReferralId())
            .offenderIds(referral.getOffenderAliases().stream()
                .map(ReferredOffenderAlias::getOffenderId)
                .collect(toSet()))
            .offenderBookIds(referral.getOffenderAliases().stream()
                .map(ReferredOffenderAlias::getOffenderBookId)
                .filter(Objects::nonNull)
                .collect(toSet()))
            .build());
    }

    public void handleDeletionComplete(final OffenderDeletionComplete event) {

        final var referral = referralRepository.findById(event.getReferralId())
            .orElseThrow(illegalState("Cannot retrieve referral record for id: '%s'", event.getReferralId()));

        checkState(Objects.equals(event.getOffenderIdDisplay(), referral.getOffenderNo()),
            "Offender number '%s' of referral '%s' does not match '%s'",
            referral.getOffenderNo(), referral.getReferralId(), event.getOffenderIdDisplay());

        if (properties.isImageRecognitionDeletionEnabled()) {
            imageDuplicationDetectionService.deleteOffenderImages(referral.getOffenderNumber());
        }

        recordDeletionCompletion(referral);
        publishDeletionCompleteEvent(referral);
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

    private void publishDeletionCompleteEvent(final OffenderDeletionReferral deletionCompletion) {

        log.info("Publishing deletion complete event for: '{}'", deletionCompletion.getOffenderNo());

        final var deletionCompleteEvent =
            uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionComplete.builder()
                .offenderIdDisplay(deletionCompletion.getOffenderNo());

        deletionCompletion.getOffenderAliases().stream()
            .collect(groupingBy(ReferredOffenderAlias::getOffenderId))
            .forEach((offenderId, bookings) -> {
                final var offenderWithBookings = OffenderWithBookings.builder().offenderId(offenderId);
                bookings.forEach(booking -> offenderWithBookings.booking(new Booking(booking.getOffenderBookId())));
                deletionCompleteEvent.offender(offenderWithBookings.build());
            });
    }
}

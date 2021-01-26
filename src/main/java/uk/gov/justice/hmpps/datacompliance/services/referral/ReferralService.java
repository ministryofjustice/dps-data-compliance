package uk.gov.justice.hmpps.datacompliance.services.referral;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderToCheck;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.AdHocOffenderDeletion;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion.OffenderAlias;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralComplete;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.AD_HOC;
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
    private final DataComplianceEventPusher deletionGrantedEventPusher;

    public void handleAdHocDeletion(final AdHocOffenderDeletion event) {

        final var batch = batchRepository.save(OffenderDeletionBatch.builder()
                .requestDateTime(timeSource.nowAsLocalDateTime())
                .commentText(event.getReason())
                .batchType(AD_HOC)
                .build());

        deletionGrantedEventPusher.requestAdHocReferral(
                new OffenderNumber(event.getOffenderIdDisplay()), batch.getBatchId());
    }

    public void handlePendingDeletionReferral(final OffenderPendingDeletion event) {

        final var referral = createReferral(event);

        final var retentionChecks = retentionService.conductRetentionChecks(
                OffenderToCheck.builder()
                        .offenderNumber(new OffenderNumber(event.getOffenderIdDisplay()))
                        .offenceCodes(event.getOffenceCodes())
                        .alertCodes(event.getAlertCodes())
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
                .receivedDateTime(LocalDateTime.now())
                .offenderNo(event.getOffenderIdDisplay())
                .firstName(event.getFirstName())
                .middleName(event.getMiddleName())
                .lastName(event.getLastName())
                .birthDate(event.getBirthDate())
                .build();

        event.getOffenderAliases().forEach(alias ->
                transform(alias).forEach(referral::addReferredOffenderAlias));

        return referral;
    }

    private List<ReferredOffenderAlias> transform(final OffenderAlias alias) {

        if (alias.getOffenderBookings().isEmpty()) {
            return List.of(ReferredOffenderAlias.builder()
                    .offenderId(alias.getOffenderId())
                    .build());
        }

        return alias.getOffenderBookings().stream()
                .map(booking -> ReferredOffenderAlias.builder()
                        .offenderId(alias.getOffenderId())
                        .offenderBookId(booking.getOffenderBookId())
                        .build())
                .collect(toList());
    }
}

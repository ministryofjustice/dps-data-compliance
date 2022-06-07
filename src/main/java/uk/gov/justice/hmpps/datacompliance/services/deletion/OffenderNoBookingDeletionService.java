package uk.gov.justice.hmpps.datacompliance.services.deletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderNoBookingDeletionResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderNoBookingDeletionResult.Offender;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderNoBookingDeletionResult.OffenderAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.OffenderNoBookingDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.OffenderNoBookingDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.offendernobooking.ReferredOffenderNoBookingAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.offendernobooking.OffenderNoBookingDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.offendernobooking.OffenderNoBookingDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.List;

import static org.springframework.util.CollectionUtils.isEmpty;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class OffenderNoBookingDeletionService {

    private final TimeSource timeSource;
    private final OffenderNoBookingDeletionBatchRepository batchRepository;
    private final OffenderNoBookingDeletionReferralRepository referralRepository;

    public void handleOffenderNoBookingDeletionResult(final OffenderNoBookingDeletionResult result) {

        log.info("Handling offender no booking deletion result");

        final var batch = batchRepository.findById(result.getBatchId())
            .orElseThrow(illegalState("Cannot find batch with id: '%s'", result.getBatchId()));

        batch.setReferralCompletionDateTime(timeSource.nowAsLocalDateTime());

        final var updatedBatch = batchRepository.save(batch);

        if (isEmpty(result.getOffenders()))
            log.info("There are no offenders with no booking that met the deletion criteria  {}", batch.getBatchId());

        else result.getOffenders().stream()
            .map(referral -> toReferralEntity(referral, updatedBatch))
            .forEach(referralRepository::save);


    }

    private OffenderNoBookingDeletionReferral toReferralEntity(Offender offender, OffenderNoBookingDeletionBatch updatedBatch) {
        final var referral = OffenderNoBookingDeletionReferral.builder()
            .offenderNoBookingDeletionBatch(updatedBatch)
            .offenderNo(offender.getOffenderIdDisplay())
            .firstName(offender.getFirstName())
            .middleName(offender.getMiddleName())
            .lastName(offender.getLastName())
            .birthDate(offender.getBirthDate())
            .deletionDateTime(offender.getDeletionDateTime())
            .build();

        offender.getOffenderAliases()
            .forEach(alias -> transform(alias)
                .forEach(referral::addReferredOffenderAlias));

        return referral;
    }


    private List<ReferredOffenderNoBookingAlias> transform(final OffenderAlias alias) {
        return List.of(ReferredOffenderNoBookingAlias.builder()
            .offenderId(alias.getOffenderId())
            .build());
    }

}


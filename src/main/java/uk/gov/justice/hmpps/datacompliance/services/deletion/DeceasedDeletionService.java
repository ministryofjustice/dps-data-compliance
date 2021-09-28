package uk.gov.justice.hmpps.datacompliance.services.deletion;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DeceasedOffenderDeletionResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DeceasedOffenderDeletionResult.DeceasedOffender;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DeceasedOffenderDeletionResult.OffenderAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.ReferredDeceasedOffenderAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender.DeceasedOffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender.DeceasedOffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class DeceasedDeletionService {

    private final TimeSource timeSource;
    private final DeceasedOffenderDeletionBatchRepository batchRepository;
    private final DeceasedOffenderDeletionReferralRepository referralRepository;

    public void handleDeceasedOffenderDeletionResult(final DeceasedOffenderDeletionResult result) {

        log.info("Handling deceased offender deletion result");

        final var batch = batchRepository.findById(result.getBatchId())
            .orElseThrow(illegalState("Cannot find batch with id: '%s'", result.getBatchId()));

        batch.setReferralCompletionDateTime(timeSource.nowAsLocalDateTime());

        final var updatedBatch = batchRepository.save(batch);

        if (result.getDeceasedOffenders() != null) {
            result.getDeceasedOffenders().stream()
                .map(referral -> toReferralEntity(referral, updatedBatch))
                .forEach(referralRepository::save);
        } else {
            log.info("There are no deceased offenders that met the deletion criteria  {}", batch.getBatchId());
        }


    }

    private DeceasedOffenderDeletionReferral toReferralEntity(DeceasedOffender deceasedOffender, DeceasedOffenderDeletionBatch updatedBatch) {
        final var referral = DeceasedOffenderDeletionReferral.builder()
            .deceasedOffenderDeletionBatch(updatedBatch)
            .offenderNo(deceasedOffender.getOffenderIdDisplay())
            .firstName(deceasedOffender.getFirstName())
            .middleName(deceasedOffender.getMiddleName())
            .lastName(deceasedOffender.getLastName())
            .birthDate(deceasedOffender.getBirthDate())
            .deceasedDate(deceasedOffender.getDeceasedDate())
            .agencyLocationId(deceasedOffender.getAgencyLocationId())
            .deletionDateTime(deceasedOffender.getDeletionDateTime())
            .build();

        deceasedOffender.getOffenderAliases()
            .forEach(alias -> transform(alias)
                .forEach(referral::addReferredOffenderAlias));

        return referral;
    }


    private List<ReferredDeceasedOffenderAlias> transform(final OffenderAlias alias) {

        if (alias.getOffenderBookIds().isEmpty()) {
            return List.of(ReferredDeceasedOffenderAlias.builder()
                .offenderId(alias.getOffenderId())
                .build());
        }

        return alias.getOffenderBookIds().stream()
            .map(bookId -> ReferredDeceasedOffenderAlias.builder()
                .offenderId(alias.getOffenderId())
                .offenderBookId((bookId))
                .build())
            .collect(toList());
    }

}


package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DataDuplicateResult;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckPathfinder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.RetentionCheckRepository;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.referral.ReferralResolutionService;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@AllArgsConstructor
public class RetentionService {

    private final PathfinderApiClient pathfinderApiClient;
    private final ManualRetentionService manualRetentionService;
    private final ImageDuplicationDetectionService imageDuplicationDetectionService;
    private final DataDuplicationDetectionService dataDuplicationDetectionService;
    private final RetentionCheckRepository retentionCheckRepository;
    private final ReferralResolutionService referralResolutionService;

    public List<ActionableRetentionCheck> conductRetentionChecks(final OffenderNumber offenderNumber) {

        // TODO GDPR-112 complete the following checks
        //  * Moratoria

        return List.of(
                pathfinderReferralCheck(offenderNumber),
                manualRetentionCheck(offenderNumber),
                imageDuplicateCheck(offenderNumber),
                dataDuplicateCheck(offenderNumber));
    }

    public void handleDataDuplicateResult(final DataDuplicateResult result) {

        final var retentionCheck = findRetentionCheck(result.getRetentionCheckId(), RetentionCheckDataDuplicate.class);
        final var referredOffenderNo = retentionCheck.getOffenderNumber();
        final var duplicateOffenderNos = result.getDuplicateOffenders().stream()
                .map(OffenderNumber::new)
                .collect(toList());

        checkState(Objects.equals(result.getOffenderIdDisplay(), referredOffenderNo.getOffenderNumber()),
                "Offender number '%s' of result '%s' does not match '%s'",
                result.getOffenderIdDisplay(), result.getRetentionCheckId(), referredOffenderNo);

        retentionCheck.addDataDuplicates(
                dataDuplicationDetectionService.persistDataDuplicates(referredOffenderNo, duplicateOffenderNos));

        retentionCheck.setCheckStatus(duplicateOffenderNos.isEmpty() ? RETENTION_NOT_REQUIRED : RETENTION_REQUIRED);

        retentionCheckRepository.save(retentionCheck);
        referralResolutionService.processUpdatedRetentionCheck(retentionCheck);
    }

    private <T extends RetentionCheck> T findRetentionCheck(final long retentionCheckId,
                                                            final Class<T> retentionCheckClass) {
        return retentionCheckRepository.findById(retentionCheckId)
                .map(retentionCheckClass::cast)
                .orElseThrow(illegalState("Cannot retrieve retention check record for id: '%s'", retentionCheckId));
    }

    private ActionableRetentionCheck pathfinderReferralCheck(final OffenderNumber offenderNumber) {

        final var check = new RetentionCheckPathfinder(pathfinderApiClient.isReferredToPathfinder(offenderNumber) ?
                RETENTION_REQUIRED : RETENTION_NOT_REQUIRED);

        return new ActionableRetentionCheck(check);
    }

    private ActionableRetentionCheck manualRetentionCheck(final OffenderNumber offenderNumber) {

        final var check = manualRetentionService.findManualOffenderRetentionWithReasons(offenderNumber)
                .map(manualRetention -> new RetentionCheckManual(RETENTION_REQUIRED).setManualRetention(manualRetention))
                .orElseGet(() -> new RetentionCheckManual(RETENTION_NOT_REQUIRED));

        return new ActionableRetentionCheck(check);
    }

    private ActionableRetentionCheck imageDuplicateCheck(final OffenderNumber offenderNumber) {

        final var imageDuplicates = imageDuplicationDetectionService.findDuplicatesFor(offenderNumber);

        final var check = imageDuplicates.isEmpty() ?
                new RetentionCheckImageDuplicate(RETENTION_NOT_REQUIRED) :
                new RetentionCheckImageDuplicate(RETENTION_REQUIRED).addImageDuplicates(imageDuplicates);

        return new ActionableRetentionCheck(check);
    }

    private ActionableRetentionCheck dataDuplicateCheck(final OffenderNumber offenderNumber) {
        return new ActionableRetentionCheck(new RetentionCheckDataDuplicate(PENDING))
                .setPendingCheck(retentionCheck -> dataDuplicationDetectionService.searchForDuplicates(
                        offenderNumber, retentionCheck.getRetentionCheckId()));
    }
}

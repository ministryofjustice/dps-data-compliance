package uk.gov.justice.hmpps.datacompliance.services.retention;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.DuplicateResult;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderToCheck;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DataDuplicateResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.FreeTextSearchResult;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckAnalyticalPlatformDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDatabaseDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckFreeTextSearch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckIdDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckOffenceCode;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckPathfinder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.RetentionCheckRepository;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.referral.ReferralResolutionService;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.ID;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.DISABLED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.illegalState;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class RetentionService {

    private static final double MAXIMUM_CONFIDENCE = 100.0;

    private final PathfinderApiClient pathfinderApiClient;
    private final ManualRetentionService manualRetentionService;
    private final ImageDuplicationDetectionService imageDuplicationDetectionService;
    private final DataDuplicationDetectionService dataDuplicationDetectionService;
    private final RetentionCheckRepository retentionCheckRepository;
    private final ReferralResolutionService referralResolutionService;
    private final MoratoriumCheckService moratoriumCheckService;
    private final DataComplianceProperties dataComplianceProperties;

    public List<ActionableRetentionCheck> conductRetentionChecks(final OffenderToCheck offenderToCheck) {

        final var offenderNumber = offenderToCheck.getOffenderNumber();

        return List.of(
                pathfinderReferralCheck(offenderNumber),
                manualRetentionCheck(offenderNumber),
                imageDuplicateCheck(offenderNumber),
                idDataDuplicateCheck(offenderNumber),
                databaseDataDuplicateCheck(offenderNumber),
                analyticalPlatformDataDuplicateCheck(offenderNumber),
                freeTextSearch(offenderNumber),
                offenceCodeCheck(offenderToCheck));
    }

    public void handleDataDuplicateResult(final DataDuplicateResult result, final Method method) {

        final var retentionCheck = findRetentionCheck(result.getRetentionCheckId(), RetentionCheckDataDuplicate.class);
        final var referredOffenderNo = retentionCheck.getOffenderNumber();
        final var duplicateOffenderNos = result.getDuplicateOffenders().stream()
                .map(OffenderNumber::new)
                .map(duplicate -> new DuplicateResult(duplicate, ID == method ? MAXIMUM_CONFIDENCE : null))
                .collect(toList());

        checkState(Objects.equals(result.getOffenderIdDisplay(), referredOffenderNo.getOffenderNumber()),
                "Offender number '%s' of result '%s' does not match '%s'",
                result.getOffenderIdDisplay(), result.getRetentionCheckId(), referredOffenderNo);

        retentionCheck.addDataDuplicates(
                dataDuplicationDetectionService.persistDataDuplicates(referredOffenderNo, duplicateOffenderNos, method));

        retentionCheck.setCheckStatus(duplicateOffenderNos.isEmpty() ? RETENTION_NOT_REQUIRED : RETENTION_REQUIRED);

        retentionCheckRepository.save(retentionCheck);
        referralResolutionService.processUpdatedRetentionCheck(retentionCheck);
    }

    public void handleFreeTextSearchResult(final FreeTextSearchResult result) {

        final var retentionCheck = findRetentionCheck(result.getRetentionCheckId(), RetentionCheckFreeTextSearch.class);
        final var referredOffenderNo = retentionCheck.getOffenderNumber();

        checkState(Objects.equals(result.getOffenderIdDisplay(), referredOffenderNo.getOffenderNumber()),
                "Offender number '%s' of result '%s' does not match '%s'",
                result.getOffenderIdDisplay(), result.getRetentionCheckId(), referredOffenderNo);

        if (!result.getMatchingTables().isEmpty()) {
            log.info("The following tables for offender: '{}' matched the free text search: {}",
                    referredOffenderNo.getOffenderNumber(), result.getMatchingTables());
        }

        retentionCheck.setCheckStatus(result.getMatchingTables().isEmpty() ? RETENTION_NOT_REQUIRED : RETENTION_REQUIRED);

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

        if (!dataComplianceProperties.isImageDuplicateCheckEnabled()) {
            return new ActionableRetentionCheck(new RetentionCheckImageDuplicate(DISABLED));
        }

        final var imageDuplicates = imageDuplicationDetectionService.findDuplicatesFor(offenderNumber);

        final var check = imageDuplicates.isEmpty() ?
                new RetentionCheckImageDuplicate(RETENTION_NOT_REQUIRED) :
                new RetentionCheckImageDuplicate(RETENTION_REQUIRED).addImageDuplicates(imageDuplicates);

        return new ActionableRetentionCheck(check);
    }

    private ActionableRetentionCheck idDataDuplicateCheck(final OffenderNumber offenderNumber) {

        if (!dataComplianceProperties.isIdDataDuplicateCheckEnabled()) {
            return new ActionableRetentionCheck(new RetentionCheckIdDataDuplicate(DISABLED));
        }

        return new ActionableRetentionCheck(new RetentionCheckIdDataDuplicate(PENDING))
                .setPendingCheck(retentionCheck -> dataDuplicationDetectionService.searchForIdDuplicates(
                        offenderNumber, retentionCheck.getRetentionCheckId()));
    }

    private ActionableRetentionCheck databaseDataDuplicateCheck(final OffenderNumber offenderNumber) {

        if (!dataComplianceProperties.isDatabaseDataDuplicateCheckEnabled()) {
            return new ActionableRetentionCheck(new RetentionCheckDatabaseDataDuplicate(DISABLED));
        }

        return new ActionableRetentionCheck(new RetentionCheckDatabaseDataDuplicate(PENDING))
                .setPendingCheck(retentionCheck -> dataDuplicationDetectionService.searchForDatabaseDuplicates(
                        offenderNumber, retentionCheck.getRetentionCheckId()));
    }

    private ActionableRetentionCheck analyticalPlatformDataDuplicateCheck(final OffenderNumber offenderNumber) {

        if (!dataComplianceProperties.isAnalyticalPlatformDataDuplicateCheckEnabled()) {
            return new ActionableRetentionCheck(new RetentionCheckAnalyticalPlatformDataDuplicate(DISABLED));
        }

        final var duplicates = dataDuplicationDetectionService.searchForAnalyticalPlatformDuplicates(offenderNumber);

        final var check = duplicates.isEmpty() ?
                new RetentionCheckAnalyticalPlatformDataDuplicate(RETENTION_NOT_REQUIRED) :
                new RetentionCheckAnalyticalPlatformDataDuplicate(RETENTION_REQUIRED)
                        .addDataDuplicates(duplicates);

        return new ActionableRetentionCheck(check);
    }

    private ActionableRetentionCheck freeTextSearch(final OffenderNumber offenderNumber) {

        return new ActionableRetentionCheck(new RetentionCheckFreeTextSearch(PENDING))
                .setPendingCheck(retentionCheck -> moratoriumCheckService.requestFreeTextSearch(
                        offenderNumber, retentionCheck.getRetentionCheckId()));
    }

    private ActionableRetentionCheck offenceCodeCheck(final OffenderToCheck offenderToCheck) {
        return new ActionableRetentionCheck(new RetentionCheckOffenceCode(
                moratoriumCheckService.retainDueToOffence(offenderToCheck) ? RETENTION_REQUIRED : RETENTION_NOT_REQUIRED));
    }
}

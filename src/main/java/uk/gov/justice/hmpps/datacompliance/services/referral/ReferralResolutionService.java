package uk.gov.justice.hmpps.datacompliance.services.referral;

import com.google.common.annotations.VisibleForTesting;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.ReferralResolutionRepository;
import uk.gov.justice.hmpps.datacompliance.services.deletion.DeletionService;
import uk.gov.justice.hmpps.datacompliance.services.retention.ActionableRetentionCheck;
import uk.gov.justice.hmpps.datacompliance.services.retention.FalsePositiveCheckService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static javax.persistence.LockModeType.PESSIMISTIC_WRITE;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.RETAINED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.DISABLED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.FALSE_POSITIVE;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckAnalyticalPlatformDataDuplicate.DATA_DUPLICATE_AP;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDatabaseDataDuplicate.DATA_DUPLICATE_DB;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class ReferralResolutionService {

    private final TimeSource timeSource;
    private final DeletionService deletionService;
    private final OffenderDeletionReferralRepository referralRepository;
    private final ReferralResolutionRepository referralResolutionRepository;
    private final FalsePositiveCheckService falsePositiveCheckService;

    public void processReferral(final OffenderDeletionReferral referral,
                                final List<ActionableRetentionCheck> actionableRetentionChecks) {

        final var retentionChecks = actionableRetentionChecks.stream()
                .map(ActionableRetentionCheck::getRetentionCheck)
                .collect(toList());

        checkState(retentionChecks.stream().anyMatch(check -> !check.isStatus(DISABLED)),
                "No retention checks have been conducted for offender: '%s'", referral.getOffenderNo());

        final var resolution = findResolution(retentionChecks);

        persistRetentionChecks(referral, retentionChecks, resolution);

        if (resolution == PENDING) {
            actionableRetentionChecks.forEach(ActionableRetentionCheck::triggerPendingCheck);
            return;
        }

        if (resolution == DELETION_GRANTED) {
            deletionService.grantDeletion(referral);
        }
    }

    public void processUpdatedRetentionCheck(final RetentionCheck retentionCheck) {

        final var referralResolution =
                referralResolutionRepository.findById(retentionCheck.getReferralResolution().getResolutionId())
                        .orElseThrow();

        // Ensure no race condition when we check other retention check statuses:
        referralResolutionRepository.lock(referralResolution, PESSIMISTIC_WRITE);

        final var resolutionStatus = findResolution(referralResolution.getRetentionChecks());

        log.info("Updating offender referral '{}' to resolution status : '{}'",
                referralResolution.getOffenderNumber(), resolutionStatus);

        referralResolution.setResolutionStatus(resolutionStatus);
        referralResolution.setResolutionDateTime(timeSource.nowAsLocalDateTime());
        referralResolutionRepository.save(referralResolution);

        if (resolutionStatus == DELETION_GRANTED) {
            deletionService.grantDeletion(referralResolution.getOffenderDeletionReferral());
        }
    }

    @VisibleForTesting
    ResolutionStatus findResolution(final List<RetentionCheck> retentionChecks) {

        if (anyPending(retentionChecks)) {
            return PENDING;
        }

        findPotentialFalsePositiveRetention(retentionChecks)
                .filter(falsePositiveCheckService::isFalsePositive)
                .ifPresent(this::markAsFalsePositive);

        if (canGrantDeletion(retentionChecks)) {
            return DELETION_GRANTED;
        }

        return RETAINED;
    }

    private boolean anyPending(final List<RetentionCheck> retentionChecks) {
        return retentionChecks.stream().anyMatch(RetentionCheck::isPending);
    }

    private Optional<RetentionCheckDataDuplicate> findPotentialFalsePositiveRetention(final List<RetentionCheck> retentionChecks) {

        final var dataDuplicateRetentions = retentionChecks.stream()
                .filter(check -> check.isStatus(RETENTION_REQUIRED))
                .filter(check -> check.isType(DATA_DUPLICATE_DB) || check.isType(DATA_DUPLICATE_AP))
                .map(RetentionCheckDataDuplicate.class::cast)
                .collect(toList());

        return dataDuplicateRetentions.size() == 1 ? dataDuplicateRetentions.stream().findFirst() : Optional.empty();
    }

    private void markAsFalsePositive(final RetentionCheck check) {
        log.debug("Check causing retention: '{}' has been found to be a false positive", check.getRetentionCheckId());
        check.setCheckStatus(FALSE_POSITIVE);
    }

    private boolean canGrantDeletion(final List<RetentionCheck> retentionChecks) {
        return retentionChecks.stream().allMatch(check ->
                check.isStatus(RETENTION_NOT_REQUIRED)
                        || check.isStatus(FALSE_POSITIVE)
                        || check.isStatus(DISABLED));
    }

    private void persistRetentionChecks(final OffenderDeletionReferral referral,
                                        final List<RetentionCheck> retentionChecks,
                                        final ResolutionStatus resolutionStatus) {

        log.info("Offender referral '{}' has resolution status : '{}'", referral.getOffenderNo(), resolutionStatus);

        final var resolution = ReferralResolution.builder()
                .resolutionDateTime(timeSource.nowAsLocalDateTime())
                .resolutionStatus(resolutionStatus)
                .build();

        retentionChecks.forEach(resolution::addRetentionCheck);

        referral.setReferralResolution(resolution);
        referralRepository.save(referral);
    }
}

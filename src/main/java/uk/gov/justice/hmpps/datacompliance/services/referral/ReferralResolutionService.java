package uk.gov.justice.hmpps.datacompliance.services.referral;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.ReferralResolutionRepository;
import uk.gov.justice.hmpps.datacompliance.services.deletion.DeletionService;
import uk.gov.justice.hmpps.datacompliance.services.retention.ActionableRetentionCheck;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import javax.persistence.EntityManager;
import java.util.List;

import static com.google.common.base.Preconditions.checkState;
import static java.util.stream.Collectors.toList;
import static javax.persistence.LockModeType.PESSIMISTIC_WRITE;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.RETAINED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;

@Slf4j
@Service
@Transactional
@AllArgsConstructor
public class ReferralResolutionService {

    private final TimeSource timeSource;
    private final EntityManager entityManager;
    private final DeletionService deletionService;
    private final OffenderDeletionReferralRepository referralRepository;
    private final ReferralResolutionRepository referralResolutionRepository;

    public void processReferral(final OffenderDeletionReferral referral,
                                final List<ActionableRetentionCheck> actionableRetentionChecks) {

        final var retentionChecks = actionableRetentionChecks.stream()
                .map(ActionableRetentionCheck::getRetentionCheck)
                .collect(toList());

        checkState(!retentionChecks.isEmpty(),
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

        final var referralResolution = retentionCheck.getReferralResolution();

        // TODO GDPR-125 Integration test to demonstrate the pesimistic lock
        // Ensure no race condition when we check other retention check statuses:
        entityManager.lock(referralResolution, PESSIMISTIC_WRITE);

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

    private ResolutionStatus findResolution(final List<RetentionCheck> retentionChecks) {

        if (anyPending(retentionChecks)) {
            return PENDING;
        }

        if (canGrantDeletion(retentionChecks)) {
            return DELETION_GRANTED;
        }

        return RETAINED;
    }

    private boolean anyPending(final List<RetentionCheck> retentionChecks) {
        return retentionChecks.stream().anyMatch(RetentionCheck::isPending);
    }

    private boolean canGrantDeletion(final List<RetentionCheck> retentionChecks) {
        return retentionChecks.stream().allMatch(check -> check.isStatus(RETENTION_NOT_REQUIRED));
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

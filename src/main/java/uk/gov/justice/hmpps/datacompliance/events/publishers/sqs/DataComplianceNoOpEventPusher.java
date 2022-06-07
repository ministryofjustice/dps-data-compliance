package uk.gov.justice.hmpps.datacompliance.events.publishers.sqs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.dto.DeceasedOffenderDeletionRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionGrant;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionReferralRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderNoBookingDeletionRequest;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderRestrictionCode;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@ConditionalOnExpression("!{'aws', 'localstack'}.contains('${hmpps.sqs.provider}')")
public class DataComplianceNoOpEventPusher implements DataComplianceEventPusher {

    public DataComplianceNoOpEventPusher() {
        log.info("Configured to ignore offender deletion granted events");
    }

    @Override
    public void requestReferral(final OffenderDeletionReferralRequest request) {
        log.warn("Pretending to request referral: {}", request);
    }

    @Override
    public void requestAdHocReferral(OffenderNumber offenderNo, Long batchId) {
        log.warn("Pretending to request ad hoc referral for offender: '{}' and batch: '{}'",
            offenderNo.getOffenderNumber(), batchId);
    }

    @Override
    public void requestProvisionalDeletionReferral(OffenderNumber offenderNo, Long referralId) {
        log.debug("Pretending to request provisional deletion for referral: '{}' for offender:" +
            " '{}'", referralId, offenderNo.getOffenderNumber());
    }

    @Override
    public void requestIdDataDuplicateCheck(final OffenderNumber offenderNo, final Long retentionCheckId) {
        log.warn("Pretending to push ID data duplicate check '{}/{}' to queue",
            offenderNo.getOffenderNumber(), retentionCheckId);
    }

    @Override
    public void requestDatabaseDataDuplicateCheck(final OffenderNumber offenderNo, final Long retentionCheckId) {
        log.warn("Pretending to push data duplicate database check '{}/{}' to queue",
            offenderNo.getOffenderNumber(), retentionCheckId);
    }

    @Override
    public void requestFreeTextMoratoriumCheck(final OffenderNumber offenderNo,
                                               final Long retentionCheckId,
                                               final List<String> regex) {
        log.warn("Pretending to push free text moratorium check '{}/{}' to queue",
            offenderNo.getOffenderNumber(), retentionCheckId);
    }

    @Override
    public void requestOffenderRestrictionCheck(final OffenderNumber offenderNumber, final Long retentionCheckId, final Set<OffenderRestrictionCode> offenderRestrictionCode, final String regex) {

        log.debug("Pretending to push restriction check for: '{}/{}'", offenderNumber.getOffenderNumber(), retentionCheckId);
    }

    @Override
    public void grantDeletion(final OffenderDeletionGrant offenderDeletionGrant) {
        log.warn("Pretending to push offender deletion granted events for '{}/{}' to queue",
            offenderDeletionGrant.getOffenderNumber().getOffenderNumber(), offenderDeletionGrant.getReferralId());
    }

    @Override
    public void requestDeceasedOffenderDeletion(DeceasedOffenderDeletionRequest request) {
        log.warn("Pretending to request deceased deletion referral: {}", request);
    }

    @Override
    public void requestOffenderNoBookingDeletion(final OffenderNoBookingDeletionRequest request) {
        log.warn("Pretending to request offender no booking deletion referral: {}", request);
    }
}

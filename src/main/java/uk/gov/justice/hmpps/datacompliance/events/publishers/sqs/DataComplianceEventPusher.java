package uk.gov.justice.hmpps.datacompliance.events.publishers.sqs;

import uk.gov.justice.hmpps.datacompliance.dto.DeceasedOffenderDeletionRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionGrant;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionReferralRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderRestrictionCode;

import java.util.List;
import java.util.Set;

public interface DataComplianceEventPusher {
    void requestReferral(OffenderDeletionReferralRequest request);
    void requestAdHocReferral(OffenderNumber offenderNumber, Long batchId);
    void requestProvisionalDeletionReferral(OffenderNumber offenderNumber, Long referralId);
    void requestIdDataDuplicateCheck(OffenderNumber offenderNumber, Long retentionCheckId);
    void requestDatabaseDataDuplicateCheck(OffenderNumber offenderNumber, Long retentionCheckId);
    void requestFreeTextMoratoriumCheck(OffenderNumber offenderNumber, Long retentionCheckId, List<String> regex);
    void requestOffenderRestrictionCheck(OffenderNumber offenderNumber, Long retentionCheckId, Set<OffenderRestrictionCode> offenderRestrictionCodes, String regex);
    void grantDeletion(OffenderDeletionGrant offenderDeletionGrant);
    void requestDeceasedOffenderDeletion(DeceasedOffenderDeletionRequest request);
}

package uk.gov.justice.hmpps.datacompliance.events.publishers.sqs;

import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionGrant;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import java.util.List;

public interface DataComplianceEventPusher {
    void grantDeletion(OffenderDeletionGrant offenderDeletionGrant);
    void requestIdDataDuplicateCheck(OffenderNumber offenderNumber, Long retentionCheckId);
    void requestDatabaseDataDuplicateCheck(OffenderNumber offenderNumber, Long retentionCheckId);
    void requestFreeTextMoratoriumCheck(OffenderNumber offenderNumber, Long retentionCheckId, List<String> regex);
}

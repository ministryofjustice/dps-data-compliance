package uk.gov.justice.hmpps.datacompliance.events.publishers.sqs;

import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

public interface DataComplianceEventPusher {
    void grantDeletion(OffenderNumber offenderNumber, Long referralId);
    void requestIdDataDuplicateCheck(OffenderNumber offenderNumber, Long retentionCheckId);
    void requestDatabaseDataDuplicateCheck(OffenderNumber offenderNumber, Long retentionCheckId);
    void requestFreeTextMoratoriumCheck(OffenderNumber offenderNumber, Long retentionCheckId, String regex);
}

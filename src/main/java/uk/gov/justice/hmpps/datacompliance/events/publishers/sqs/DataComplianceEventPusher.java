package uk.gov.justice.hmpps.datacompliance.events.publishers.sqs;

import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

public interface DataComplianceEventPusher {
    void grantDeletion(final OffenderNumber offenderNumber, final Long referralId);
    void requestIdDataDuplicateCheck(final OffenderNumber offenderNumber, final Long retentionCheckId);
    void requestDatabaseDataDuplicateCheck(final OffenderNumber offenderNumber, final Long retentionCheckId);
}

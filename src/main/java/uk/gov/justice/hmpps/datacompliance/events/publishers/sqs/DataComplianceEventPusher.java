package uk.gov.justice.hmpps.datacompliance.events.publishers.sqs;

import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

public interface DataComplianceEventPusher {
    void grantDeletion(final OffenderNumber offenderNumber, final Long referralId);
    void requestDataDuplicateCheck(final OffenderNumber offenderNumber, final Long retentionCheckId);
}

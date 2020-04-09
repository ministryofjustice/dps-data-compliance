package uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted;

import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

public interface OffenderDeletionGrantedEventPusher {
    void grantDeletion(final OffenderNumber offenderNumber, final Long referralId);
}

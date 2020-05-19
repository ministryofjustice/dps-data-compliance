package uk.gov.justice.hmpps.datacompliance.events.publishers.sns;

import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionComplete;

public interface OffenderDeletionCompleteEventPusher {
    void sendEvent(OffenderDeletionComplete event);
}

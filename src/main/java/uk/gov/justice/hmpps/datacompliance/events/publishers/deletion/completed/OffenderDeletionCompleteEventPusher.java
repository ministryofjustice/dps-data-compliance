package uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.completed;

import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent;

public interface OffenderDeletionCompleteEventPusher {
    void sendEvent(OffenderDeletionCompleteEvent event);
}

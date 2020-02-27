package uk.gov.justice.hmpps.datacompliance.services.events.publishers.deletion.completed;

import uk.gov.justice.hmpps.datacompliance.services.events.dto.OffenderDeletionCompleteEvent;

public interface OffenderDeletionCompleteEventPusher {
    void sendEvent(OffenderDeletionCompleteEvent event);
}

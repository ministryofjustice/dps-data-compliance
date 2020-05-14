package uk.gov.justice.hmpps.datacompliance.events.publishers.sns;

import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent;

public interface OffenderDeletionCompleteEventPusher {
    void sendEvent(OffenderDeletionCompleteEvent event);
}

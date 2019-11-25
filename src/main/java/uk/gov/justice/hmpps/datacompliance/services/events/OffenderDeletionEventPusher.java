package uk.gov.justice.hmpps.datacompliance.services.events;

public interface OffenderDeletionEventPusher {
    void sendEvent(final String offenderIdDisplay);
}

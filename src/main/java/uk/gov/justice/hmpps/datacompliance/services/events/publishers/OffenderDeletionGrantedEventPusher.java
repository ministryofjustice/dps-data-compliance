package uk.gov.justice.hmpps.datacompliance.services.events.publishers;

public interface OffenderDeletionGrantedEventPusher {
    void sendEvent(final String offenderIdDisplay);
}

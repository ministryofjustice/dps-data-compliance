package uk.gov.justice.hmpps.datacompliance.services.events.publishers.deletion.granted;

public interface OffenderDeletionGrantedEventPusher {
    void sendEvent(final String offenderIdDisplay);
}

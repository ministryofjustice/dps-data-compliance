package uk.gov.justice.hmpps.datacompliance.services;

public interface OffenderDeletionEventPusher {
    void sendEvent(final String offenderIdDisplay);
}

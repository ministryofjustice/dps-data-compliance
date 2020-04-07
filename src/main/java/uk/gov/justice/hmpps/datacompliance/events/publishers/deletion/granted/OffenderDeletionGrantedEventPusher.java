package uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted;

public interface OffenderDeletionGrantedEventPusher {
    void grantDeletion(final String offenderIdDisplay);
}

package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.request.Request.*;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.*;

@TestMethodOrder(OrderAnnotation.class)
public class ReviewRequiredMessagesIntegrationTest extends QueueIntegrationTest {

    @Test
    @Order(1)
    public void shouldAllowReviewPeriodWhenReviewIsRequired() {

        final var offenderIdDisplay = "A1234PO";
        final var batch = persistNewBatch();

        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));
        communityApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        mockJmsListener.respondToRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234PO");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyNoMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);

        waitUntilResponseQueueMessagesAreConsumed();
        waitUntilResolutionStatusIsPersisted(offenderIdDisplay, "PROVISIONAL_DELETION_GRANTED");

        mockJmsListener.clearMessages();

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));
        communityApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        final var referralId = offenderDeletionReferralRepository.findByOffenderNo(offenderIdDisplay).stream().findFirst().get().getReferralId();
        final var provisionalDeletionReferral = forProvisionalDeletionReferral(sqsResponseClientQueueUrl, referralId, offenderIdDisplay);

        mockJmsListener.respondToRequestWith(Set.of(provisionalDeletionReferral));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234PO");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var secondDataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, secondDataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var secondFreeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, secondFreeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var secondOffenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, secondOffenderRestrictionCheckId, false)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var secondDataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, secondDataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        waitUntilResolutionStatusIsPersisted(referralId, "DELETION_GRANTED");

        mockJmsListener.respondToRequestWith(Set.of(forOffenderDeletionCompleteResult(sqsResponseClientQueueUrl, referralId, offenderIdDisplay)));

        waitUntilResponseQueueMessagesAreConsumed();
        waitUntilResolutionStatusIsPersisted(referralId, "DELETED");
    }

    @Test
    @Order(2)
    public void shouldRetainOffenderWhenARetentionCheckFromSecondIterationComesBackPositive() {

        final var offenderIdDisplay = "B1234PO";
        final var batch = persistNewBatch();

        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));
        communityApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        mockJmsListener.respondToRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/B1234PO");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyNoMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);

        waitUntilResponseQueueMessagesAreConsumed();
        waitUntilResolutionStatusIsPersisted(offenderIdDisplay, "PROVISIONAL_DELETION_GRANTED");

        mockJmsListener.clearMessages();

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));
        communityApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));

        final var referralId = offenderDeletionReferralRepository.findByOffenderNo(offenderIdDisplay).stream().findFirst().get().getReferralId();
        final var provisionalDeletionReferral = forProvisionalDeletionReferral(sqsResponseClientQueueUrl, referralId, offenderIdDisplay);

        mockJmsListener.respondToRequestWith(Set.of(provisionalDeletionReferral));

        waitForPathFinderApiRequestTo("/pathfinder/offender/B1234PO");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var secondDataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, secondDataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var secondFreeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, secondFreeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var secondOffenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, secondOffenderRestrictionCheckId, false)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var secondDataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, secondDataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyNoMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        waitUntilResolutionStatusIsPersisted(offenderIdDisplay, "RETAINED");
    }


}

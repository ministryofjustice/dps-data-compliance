package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.request.Request.AD_HOC_REFERRAL_REQUEST;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.request.Request.DATA_DUPLICATE_DB_CHECK;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.request.Request.DATA_DUPLICATE_ID_CHECK;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.request.Request.FREE_TEXT_MORATORIUM_CHECK;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.request.Request.OFFENDER_DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.request.Request.OFFENDER_RESTRICTION_CHECK;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.forAdHocDeletionEvent;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.forDataDuplicateDbResult;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.forDataDuplicateIdResult;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.forFreeTextSearchResult;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.forOffenderDeletionCompleteResult;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.forOffenderPendingDeletion;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.forOffenderRestrictionResult;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.forPendingDeletionReferralComplete;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.forProvisionalDeletionReferral;


public class ReviewRequiredAdHocMessagesIntegrationTest extends QueueIntegrationTest {

    @Test
    public void shouldGrantProvisionalDeletionWhenAdHocDeletion() {

        final var offenderIdDisplay = "A1234AZ";

        final var adHockOffenderDeletionEvent = forAdHocDeletionEvent(sqsResponseClientQueueUrl, offenderIdDisplay);
        mockJmsListener.triggerAdhocDeletion(adHockOffenderDeletionEvent);

        mockJmsListener.verifyMessageReceivedOfEventType(AD_HOC_REFERRAL_REQUEST);
        assertThat(repository.findAll().iterator()).hasNext();
        var batch = repository.findAll().iterator().next();

        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);
        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));
        communityApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        mockJmsListener.respondToRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AZ");

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

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AZ");

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



}

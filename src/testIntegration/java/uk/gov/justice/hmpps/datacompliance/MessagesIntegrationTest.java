package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.utils.web.request.ManualRetentionReasonCode;

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
import static uk.gov.justice.hmpps.datacompliance.utils.web.request.RequestFactory.forManualRetentionRequest;


public class MessagesIntegrationTest extends QueueIntegrationTest {


    //@Test
    public void shouldGrantDeletionWhenAllChecksComeBackNegative() {

        final var offenderIdDisplay = "A1234AA";
        final var batch = persistNewBatch();

        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));
        communityApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        mockJmsListener.respondToRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AA");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        final var referralId = mockJmsListener.getIdFromPayload(OFFENDER_DELETION_GRANTED, "referralId");
        mockJmsListener.respondToRequestWith(Set.of(forOffenderDeletionCompleteResult(sqsResponseClientQueueUrl, referralId, offenderIdDisplay)));

        waitUntilResponseQueueMessagesAreConsumed();
        waitUntilResolutionStatusIsPersisted(referralId, "DELETED");
    }

    @Test
    @Sql("classpath:seed.data/retention_reason_code.sql")
    public void shouldRetainOffenderWhenManualRetentionIsInvoked() {

        final var offenderIdDisplay = "A1234AB";
        final var request = forManualRetentionRequest(ManualRetentionReasonCode.HIGH_PROFILE);

        webTestClient.put().uri("/retention/offenders/A1234AB")
            .bodyValue(request)
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().valueEquals("Location", "/retention/offenders/A1234AB")
            .expectBody().isEmpty();

        final var batch = persistNewBatch();

        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));
        communityApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        mockJmsListener.respondToRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AB");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

        mockJmsListener.verifyNoMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        waitUntilResolutionStatusIsPersisted(offenderIdDisplay, "RETAINED");
    }

    @Test
    public void shouldRetainOffenderWhenARetentionCheckComesBackPositive() {

        final var offenderIdDisplay = "A1234AC";
        final var batch = persistNewBatch();

        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));
        communityApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));

        mockJmsListener.respondToRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AC");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

        mockJmsListener.verifyNoMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        waitUntilResolutionStatusIsPersisted(offenderIdDisplay, "RETAINED");
    }

    //@Test
    public void shouldAllowAdHocDeletion() {

        final var offenderIdDisplay = "A1234AD";

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

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AD");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(Set.of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        final var referralId = mockJmsListener.getIdFromPayload(OFFENDER_DELETION_GRANTED, "referralId");
        mockJmsListener.respondToRequestWith(Set.of(forOffenderDeletionCompleteResult(sqsResponseClientQueueUrl, referralId, offenderIdDisplay)));

        waitUntilResponseQueueMessagesAreConsumed();
        waitUntilResolutionStatusIsPersisted(referralId, "DELETED");
    }

}

package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;

import java.time.LocalDateTime;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.SCHEDULED;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.EventType.Request.DATA_DUPLICATE_ID_CHECK;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.EventType.Request.FREE_TEXT_MORATORIUM_CHECK;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.EventType.Request.OFFENDER_DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.EventType.Request.REFERRAL_REQUEST;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.request.SqsRequestFactory.forReferral;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.response.SqsResponseFactory.forDataDuplicateResult;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.response.SqsResponseFactory.forFreeTextSearchResult;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.response.SqsResponseFactory.forOffenderPendingDeletion;


public class MessagesIntegrationTest extends QueueIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private OffenderDeletionBatchRepository repository;


    @Test
    public void shouldGrantDeletionWhenAllChecksComeBackNegative() {

        final String offenderIdDisplay = "A1234AA";
        final var batch = persistNewBatch();

        final var referralRequest = forReferral(sqsRequestClientQueueUrl, batch);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        mockJmsListener.whenReceivedCheckWithEventType(REFERRAL_REQUEST).thenReturn(Set.of(offenderPendingDeletionResponse));

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());

        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        sqsRequestClient.sendMessage(referralRequest);
        waitUntilRequestQueueMessagesAreConsumed();

        await().until(() -> pathFinderRequestForRequestCountFor("http://localhost:8997/pathfinder/offender/A1234AA") == 1);

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final Long dataDuplicateRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToCheckRequestWith((forDataDuplicateResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final Long freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToCheckRequestWith(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
    }

    private MockResponse mockTokenAuthenticationResponse() {
        return new MockResponse()
            .setResponseCode(200)
            .setBody("{\"access_token\":\"123\",\"token_type\":\"bearer\",\"expires_in\":\"999999\"}")
            .setHeader("Content-Type", "application/json");
    }

    private OffenderDeletionBatch persistNewBatch() {
        return repository.save(OffenderDeletionBatch.builder()
            .requestDateTime(NOW)
            .referralCompletionDateTime(NOW.plusSeconds(1))
            .windowStartDateTime(NOW.plusSeconds(2))
            .windowEndDateTime(NOW.plusSeconds(3))
            .batchType(SCHEDULED)
            .build());
    }


}

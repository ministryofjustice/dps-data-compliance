package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.awaitility.Awaitility.await;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.SCHEDULED;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.EventType.Request.*;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.request.SqsRequestFactory.forReferral;
import static uk.gov.justice.hmpps.datacompliance.utils.sqs.response.SqsResponseFactory.*;


public class MessagesIntegrationTest extends QueueIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private OffenderDeletionBatchRepository repository;


    @Test
    public void shouldGrantDeletionWhenAllChecksComeBackNegative() {

        final String offenderIdDisplay = "A1234AA";
        final var batch = persistNewBatch();

        final var referralRequest = forReferral(sqsRequestClientQueueUrl, batch);
        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsRequestClientQueueUrl, batch.getBatchId(), 1L, 1L);

        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        mockJmsListener.whenReceivedCheckWithEventType(REFERRAL_REQUEST).thenReturn(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        sqsRequestClient.sendMessage(referralRequest);
        waitUntilRequestQueueMessagesAreConsumed();

        await().atMost(Duration.ofSeconds(60)).until(() -> pathFinderRequestForRequestCountFor("http://localhost:8997/pathfinder/offender/A1234AA") == 1);

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final Long dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToCheckRequestWith((forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final Long dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToCheckRequestWith((forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final Long freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToCheckRequestWith(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        final Long referralId = mockJmsListener.getIdFromPayload(OFFENDER_DELETION_GRANTED, "referralId");
        mockJmsListener.respondToCheckRequestWith(forOffenderDeletionCompleteResult(sqsResponseClientQueueUrl, referralId, offenderIdDisplay));

        // SNS verification
    }

    @Test
    public void shouldRetainOffenderWhenManualRetentionIsInvoked(){


    }

    @Test
    public void shouldRetainOffenderWhenARetentionCheckComesBackPositive(){


    }

    @Test
    public void shouldAllowAdHocDeletion(){


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

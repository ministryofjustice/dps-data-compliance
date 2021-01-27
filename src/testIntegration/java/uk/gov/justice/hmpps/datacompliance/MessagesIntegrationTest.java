package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.web.JwtAuthenticationHelper;
import uk.gov.justice.hmpps.datacompliance.utils.web.request.ManualRetentionReasonCode;

import java.time.LocalDateTime;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.SCHEDULED;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.request.Request.*;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.*;
import static uk.gov.justice.hmpps.datacompliance.utils.web.request.RequestFactory.forManualRetentionRequest;

@DirtiesContext(classMode = ClassMode.BEFORE_EACH_TEST_METHOD)
public class MessagesIntegrationTest extends QueueIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @BeforeEach
    public void clearMockJmsListener(){
        mockJmsListener.clearMessages();
    }

    @Autowired
    private OffenderDeletionBatchRepository repository;

    @Autowired
    private JwtAuthenticationHelper jwtAuthenticationHelper;

    @Test
    public void shouldGrantDeletionWhenAllChecksComeBackNegative() {

        final var offenderIdDisplay = "A1234AA";
        final var batch = persistNewBatch();

        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        mockJmsListener.respondToCheckRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AA");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        final var referralId = mockJmsListener.getIdFromPayload(OFFENDER_DELETION_GRANTED, "referralId");
        mockJmsListener.respondToCheckRequestWith(Set.of(forOffenderDeletionCompleteResult(sqsResponseClientQueueUrl, referralId, offenderIdDisplay)));
    }

    @Test
    @Sql("classpath:seed.data/retention_reason_code.sql")
    public void shouldRetainOffenderWhenManualRetentionIsInvoked() {

        final var offenderIdDisplay = "A1234AA";
        final var request = forManualRetentionRequest(ManualRetentionReasonCode.HIGH_PROFILE);

        webTestClient.put().uri("/retention/offenders/A1234AA")
            .bodyValue(request)
            .header("Authorization", "Bearer " + jwtAuthenticationHelper.createJwt())
            .exchange()
            .expectStatus().isCreated()
            .expectHeader().valueEquals("Location", "/retention/offenders/A1234AA")
            .expectBody().isEmpty();

        final var batch = persistNewBatch();

        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        mockJmsListener.respondToCheckRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AA");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyNoMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
    }

    @Test
    public void shouldRetainOffenderWhenARetentionCheckComesBackPositive() {

        final var offenderIdDisplay = "A1234AA";
        final var batch = persistNewBatch();

        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value()));

        mockJmsListener.respondToCheckRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AA");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyNoMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
    }

    @Test
    public void shouldAllowAdHocDeletion() {

        final var offenderIdDisplay = "A1234AA";

        final var adHockOffenderDeletionEvent = forAdHocDeletionEvent(sqsResponseClientQueueUrl, offenderIdDisplay);
        mockJmsListener.triggerAdhocDeletion(adHockOffenderDeletionEvent);

        mockJmsListener.verifyMessageReceivedOfEventType(AD_HOC_REFERRAL_REQUEST);
        assertThat(repository.findAll().iterator()).hasNext();
        var batch = repository.findAll().iterator().next();

        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);
        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        mockJmsListener.respondToCheckRequestWith(Set.of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AA");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToCheckRequestWith(Set.of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        final var referralId = mockJmsListener.getIdFromPayload(OFFENDER_DELETION_GRANTED, "referralId");
        mockJmsListener.respondToCheckRequestWith(Set.of(forOffenderDeletionCompleteResult(sqsResponseClientQueueUrl, referralId, offenderIdDisplay)));
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

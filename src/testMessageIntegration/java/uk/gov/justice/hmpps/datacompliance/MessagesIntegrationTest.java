package uk.gov.justice.hmpps.datacompliance;

import okhttp3.mockwebserver.MockResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.DeceasedOffenderDeletionResult.DeceasedOffender;
import uk.gov.justice.hmpps.datacompliance.utils.web.request.ManualRetentionReasonCode;

import java.time.temporal.ChronoUnit;

import static java.util.Set.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.request.Request.*;
import static uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.SqsResponseQueueFactory.*;
import static uk.gov.justice.hmpps.datacompliance.utils.web.request.RequestFactory.forManualRetentionRequest;


public class MessagesIntegrationTest extends QueueIntegrationTest {


    @Test
    public void shouldGrantDeletionWhenAllChecksComeBackNegative() {

        final var offenderIdDisplay = "A1234AA";
        final var batch = persistNewBatch();

        final var pendingDeletionReferralComplete = forPendingDeletionReferralComplete(sqsResponseClientQueueUrl, batch.getBatchId(), 1L, 1L);
        final var offenderPendingDeletionResponse = forOffenderPendingDeletion(sqsResponseClientQueueUrl, batch.getBatchId(), offenderIdDisplay);

        hmppsAuthMock.enqueue(mockTokenAuthenticationResponse());
        pathfinderApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));
        communityApiMock.enqueue(new MockResponse().setResponseCode(HttpStatus.NOT_FOUND.value()));

        mockJmsListener.respondToRequestWith(of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AA");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        final var referralId = mockJmsListener.getIdFromPayload(OFFENDER_DELETION_GRANTED, "referralId");
        mockJmsListener.respondToRequestWith(of(forOffenderDeletionCompleteResult(sqsResponseClientQueueUrl, referralId, offenderIdDisplay)));

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

        mockJmsListener.respondToRequestWith(of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AB");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

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

        mockJmsListener.respondToRequestWith(of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AC");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

        mockJmsListener.verifyNoMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        waitUntilResolutionStatusIsPersisted(offenderIdDisplay, "RETAINED");
    }

    @Test
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

        mockJmsListener.respondToRequestWith(of(offenderPendingDeletionResponse, pendingDeletionReferralComplete));

        waitForPathFinderApiRequestTo("/pathfinder/offender/A1234AD");

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_ID_CHECK);
        final var dataDuplicateIdRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_ID_CHECK);
        mockJmsListener.respondToRequestWith(of(forDataDuplicateIdResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateIdRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(DATA_DUPLICATE_DB_CHECK);
        final var dataDuplicateDbRetentionCheckId = mockJmsListener.getCheckId(DATA_DUPLICATE_DB_CHECK);
        mockJmsListener.respondToRequestWith(of(forDataDuplicateDbResult(sqsResponseClientQueueUrl, offenderIdDisplay, dataDuplicateDbRetentionCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(FREE_TEXT_MORATORIUM_CHECK);
        final var freeTextMoratoriumCheckId = mockJmsListener.getCheckId(FREE_TEXT_MORATORIUM_CHECK);
        mockJmsListener.respondToRequestWith(of(forFreeTextSearchResult(sqsResponseClientQueueUrl, offenderIdDisplay, freeTextMoratoriumCheckId)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_RESTRICTION_CHECK);
        final var offenderRestrictionCheckId = mockJmsListener.getCheckId(OFFENDER_RESTRICTION_CHECK);
        mockJmsListener.respondToRequestWith(of(forOffenderRestrictionResult(sqsResponseClientQueueUrl, offenderIdDisplay, offenderRestrictionCheckId, false)));

        mockJmsListener.verifyMessageReceivedOfEventType(OFFENDER_DELETION_GRANTED);
        final var referralId = mockJmsListener.getIdFromPayload(OFFENDER_DELETION_GRANTED, "referralId");
        mockJmsListener.respondToRequestWith(of(forOffenderDeletionCompleteResult(sqsResponseClientQueueUrl, referralId, offenderIdDisplay)));

        waitUntilResponseQueueMessagesAreConsumed();
        waitUntilResolutionStatusIsPersisted(referralId, "DELETED");
    }


    @Test
    public void shouldDeleteDeceasedOffenders() {

        final var offenderIdDisplay = "A1234AC";
        final var batch = persistNewDeceasedOffenderBatch();

        final var deceasedOffender = getDeceasedOffender(offenderIdDisplay);
        final var pendingDeletionReferralComplete = forDeceasedOffenderDeletionResult(sqsResponseClientQueueUrl, batch.getBatchId(), deceasedOffender);
        mockJmsListener.respondToRequestWith(of(pendingDeletionReferralComplete));

        waitUntilResponseQueueMessagesAreConsumed();
        validateReferralRecorded(deceasedOffender);
    }

    private void validateReferralRecorded(DeceasedOffender deceasedOffender) {
        final var deceasedOffenderDeletionReferrals = retrieveDeceasedReferralWithWait(deceasedOffender.getOffenderIdDisplay());

        assertThat(deceasedOffenderDeletionReferrals).hasSize(1);

        assertThat(deceasedOffenderDeletionReferrals.get(0).getFirstName()).isEqualTo(deceasedOffender.getFirstName());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getMiddleName()).isEqualTo(deceasedOffender.getMiddleName());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getLastName()).isEqualTo(deceasedOffender.getLastName());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getAgencyLocationId()).isEqualTo(deceasedOffender.getAgencyLocationId());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getDeceasedDate()).isEqualTo(deceasedOffender.getDeceasedDate());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getDeletionDateTime()).isCloseTo(deceasedOffender.getDeletionDateTime(),  within(1, ChronoUnit.SECONDS));
        assertThat(deceasedOffenderDeletionReferrals.get(0).getBirthDate()).isEqualTo(deceasedOffender.getBirthDate());

    }


}

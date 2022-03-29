package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

public class HouseKeepingIntegrationTest extends QueueIntegrationTest {


    @BeforeEach
    public void setup() {
        sqsRequestClient.purgeQueue(new PurgeQueueRequest(sqsRequestClientQueueUrl));
        sqsResponseClient.purgeQueue(new PurgeQueueRequest(sqsResponseClientQueueUrl));
    }

    @Test
    public void housekeepingWillConsumeAMessageOnTheRequestDlqAndReturnToMainQueue() {

        var message = """
            {
               "some: "message
            }"
            """;

        sqsResponseClient.sendMessage(sqsDlqRequestClientQueueUrl, message);
        Awaitility.await().until(() -> getNumberOfMessagesOnDlqRequestQueue() == 1);

        webTestClient.put()
            .uri("/queue-admin/retry-all-dlqs")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk();

        Awaitility.await().until(() -> getNumberOfMessagesOnDlqRequestQueue() == 0);
        Awaitility.await().until(() -> getNumberOfMessagesOnRequestQueue() == 1);

        final var messageResult = sqsRequestClient.receiveMessage(sqsRequestClientQueueUrl).getMessages().get(0).getBody().trim();

        assertThat(message).isEqualToIgnoringWhitespace(messageResult);
    }


    @Test
    public void housekeepingWillConsumeAMessageOnTheResponseDlqAndReturnToMainQueue() {

        final var batch = persistNewDeceasedOffenderBatch();

        final var deceasedOffender = getDeceasedOffender("A1234AC");
        final var message = forDeceasedOffenderDeletionResult(sqsDlqResponseClientQueueUrl, batch.getBatchId(), deceasedOffender);

        sqsDlqResponseClient.sendMessage(message);
        Awaitility.await().until(() -> getNumberOfMessagesOnDlqResponseQueue() == 1);

        webTestClient.put()
            .uri("/queue-admin/retry-all-dlqs")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk();

        Awaitility.await().until(() -> getNumberOfMessagesOnDlqResponseQueue() == 0);

        final var deceasedOffenderDeletionReferrals = retrieveDeceasedReferralWithWait(deceasedOffender.getOffenderIdDisplay());

        assertThat(deceasedOffenderDeletionReferrals).hasSize(1);
        assertThat(deceasedOffenderDeletionReferrals.get(0).getFirstName()).isEqualTo(deceasedOffender.getFirstName());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getMiddleName()).isEqualTo(deceasedOffender.getMiddleName());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getLastName()).isEqualTo(deceasedOffender.getLastName());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getAgencyLocationId()).isEqualTo(deceasedOffender.getAgencyLocationId());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getDeceasedDate()).isEqualTo(deceasedOffender.getDeceasedDate());
        assertThat(deceasedOffenderDeletionReferrals.get(0).getDeletionDateTime()).isCloseTo(deceasedOffender.getDeletionDateTime(), within(1, ChronoUnit.SECONDS));
        assertThat(deceasedOffenderDeletionReferrals.get(0).getBirthDate()).isEqualTo(deceasedOffender.getBirthDate());
    }

}


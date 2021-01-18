package uk.gov.justice.hmpps.datacompliance;

import org.junit.jupiter.api.Test;
import uk.gov.justice.hmpps.datacompliance.utils.sqs.EventType.Request;
import uk.gov.justice.hmpps.datacompliance.utils.sqs.SqsMessage;

import static org.assertj.core.api.Assertions.assertThat;


public class MessagesIntegrationTest extends QueueIntegrationTest {


    @Test
    public void shouldSendAndRetrieveMessagesOnFromTheQueue() {

        final String sqsMessage = new SqsMessage()
            .withEventType(Request.OFFENDER_DELETION_GRANTED)
            .withDeletionGranted("someOffenderId", 1L)
            .asJson();

        final String queueUrl = sqsRequestClient.getQueueUrl(sqsRequestQueueName).getQueueUrl();
        sqsRequestClient.sendMessage(queueUrl, sqsMessage);

        final int numberOfMessagesCurrentlyOnQueue = getNumberOfMessagesCurrentlyOnQueue(queueUrl, sqsRequestClient);
        assertThat(numberOfMessagesCurrentlyOnQueue).isGreaterThanOrEqualTo(1);
    }


}

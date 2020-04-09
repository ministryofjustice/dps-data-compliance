package uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OffenderDeletionGrantedEventPusherTest {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static OffenderNumber OFFENDER_NUMBER = new OffenderNumber("offender1");

    @Mock
    private AmazonSQS client;

    private OffenderDeletionGrantedEventPusher eventPusher;

    @BeforeEach
    void setUp() {
        eventPusher = new OffenderDeletionGrantedAwsEventPusher(client, "queue.url", OBJECT_MAPPER);
    }

    @Test
    void sendEvent() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
                .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.grantDeletion(OFFENDER_NUMBER, 123L);

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageBody()).isEqualTo("{\"offenderIdDisplay\":\"offender1\",\"referralId\":123}");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
                .isEqualTo("DATA_COMPLIANCE_OFFENDER-DELETION-GRANTED");
    }

    @Test
    void sendEventPropagatesException() {
        when(client.sendMessage(any())).thenThrow(RuntimeException.class);
        assertThatThrownBy(() -> eventPusher.grantDeletion(OFFENDER_NUMBER, 123L)).isInstanceOf(RuntimeException.class);
    }
}
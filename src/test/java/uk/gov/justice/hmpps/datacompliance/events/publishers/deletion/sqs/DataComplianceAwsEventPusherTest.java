package uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.sqs;

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
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionGrant;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceAwsEventPusher;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataComplianceAwsEventPusherTest {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private final static long REFERRAL_ID = 123;
    private final static long OFFENDER_ID = 456;
    private final static long OFFENDER_BOOK_ID = 789;

    @Mock
    private AmazonSQS client;

    private DataComplianceEventPusher eventPusher;

    @BeforeEach
    void setUp() {
        eventPusher = new DataComplianceAwsEventPusher(client, "queue.url", OBJECT_MAPPER);
    }

    @Test
    void grantDeletion() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
                .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.grantDeletion(
                OffenderDeletionGrant.builder()
                        .offenderNumber(OFFENDER_NUMBER)
                        .referralId(REFERRAL_ID)
                        .offenderId(OFFENDER_ID)
                        .offenderBookId(OFFENDER_BOOK_ID)
                        .build());

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
                .isEqualTo("DATA_COMPLIANCE_OFFENDER-DELETION-GRANTED");
        assertThat(request.getValue().getMessageBody()).isEqualTo(
                "{" +
                        "\"offenderIdDisplay\":\"A1234AA\"," +
                        "\"referralId\":123," +
                        "\"offenderIds\":[456]," +
                        "\"offenderBookIds\":[789]" +
                "}");
    }

    @Test
    void requestIdDataDuplicateCheck() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
                .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.requestIdDataDuplicateCheck(OFFENDER_NUMBER, 123L);

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageBody()).isEqualTo("{\"offenderIdDisplay\":\"A1234AA\",\"retentionCheckId\":123}");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
                .isEqualTo("DATA_COMPLIANCE_DATA-DUPLICATE-ID-CHECK");
    }

    @Test
    void requestDatabaseDataDuplicateCheck() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
                .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.requestDatabaseDataDuplicateCheck(OFFENDER_NUMBER, 123L);

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageBody()).isEqualTo("{\"offenderIdDisplay\":\"A1234AA\",\"retentionCheckId\":123}");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
                .isEqualTo("DATA_COMPLIANCE_DATA-DUPLICATE-DB-CHECK");
    }

    @Test
    void requestFreeTextMoratoriumCheck() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
                .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.requestFreeTextMoratoriumCheck(OFFENDER_NUMBER, 123L, List.of("^(regex|1)$","^(regex|2)$"));

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageBody()).isEqualTo("{\"offenderIdDisplay\":\"A1234AA\",\"retentionCheckId\":123,\"regex\":[\"^(regex|1)$\",\"^(regex|2)$\"]}");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
                .isEqualTo("DATA_COMPLIANCE_FREE-TEXT-MORATORIUM-CHECK");
    }
}

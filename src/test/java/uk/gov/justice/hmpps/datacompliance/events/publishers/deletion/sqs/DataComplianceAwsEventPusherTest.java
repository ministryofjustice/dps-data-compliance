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
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionReferralRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceAwsEventPusher;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.sqs.HmppsQueue;
import uk.gov.justice.hmpps.sqs.HmppsQueueService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderRestrictionCode.CHILD;

@ExtendWith(MockitoExtension.class)
class DataComplianceAwsEventPusherTest {

    private final static ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final static OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private final static long BATCH_ID = 987;
    private final static long REFERRAL_ID = 123;
    private final static long OFFENDER_ID = 456;
    private final static long OFFENDER_BOOK_ID = 789;
    private final static LocalDate REFERRAL_WINDOW_START = LocalDate.of(2020, 1, 2);
    private final static LocalDate REFERRAL_WINDOW_END = LocalDate.of(2020, 3, 4);
    private final static int REFERRAL_LIMIT = 10;

    @Mock
    HmppsQueueService hmppsQueueService;

    @Mock
    HmppsQueue hmppsQueue;

    @Mock
    private AmazonSQS client;

    private DataComplianceEventPusher eventPusher;

    @BeforeEach
    void setUp() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        eventPusher = new DataComplianceAwsEventPusher(hmppsQueueService, OBJECT_MAPPER);
        mockHmppsService();
        invokePostConstruct();
    }

    @Test
    void requestReferral() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
            .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.requestReferral(OffenderDeletionReferralRequest.builder()
                .batchId(BATCH_ID)
                .dueForDeletionWindowStart(REFERRAL_WINDOW_START)
                .dueForDeletionWindowEnd(REFERRAL_WINDOW_END)
                .limit(REFERRAL_LIMIT)
                .build());

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
                .isEqualTo("DATA_COMPLIANCE_REFERRAL-REQUEST");
        assertThat(request.getValue().getMessageBody()).isEqualTo(
                "{" +
                        "\"batchId\":987," +
                        "\"dueForDeletionWindowStart\":\"2020-01-02\"," +
                        "\"dueForDeletionWindowEnd\":\"2020-03-04\"," +
                        "\"limit\":10" +
                "}");
    }

    @Test
    void requestReferralWithoutLimit() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
                .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.requestReferral(OffenderDeletionReferralRequest.builder()
                .batchId(BATCH_ID)
                .dueForDeletionWindowStart(REFERRAL_WINDOW_START)
                .dueForDeletionWindowEnd(REFERRAL_WINDOW_END)
                .build());

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
                .isEqualTo("DATA_COMPLIANCE_REFERRAL-REQUEST");
        assertThat(request.getValue().getMessageBody()).isEqualTo(
                "{" +
                        "\"batchId\":987," +
                        "\"dueForDeletionWindowStart\":\"2020-01-02\"," +
                        "\"dueForDeletionWindowEnd\":\"2020-03-04\"" +
                "}");
    }

    @Test
    void requestAdHocReferral() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
                .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.requestAdHocReferral(OFFENDER_NUMBER, 123L);

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageBody()).isEqualTo("{\"offenderIdDisplay\":\"A1234AA\",\"batchId\":123}");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
                .isEqualTo("DATA_COMPLIANCE_AD-HOC-REFERRAL-REQUEST");
    }

    @Test
    void requestProvisionalDeletionReferral() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
            .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.requestProvisionalDeletionReferral(OFFENDER_NUMBER, 123L);

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageBody()).isEqualTo("{\"offenderIdDisplay\":\"A1234AA\",\"referralId\":123}");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
            .isEqualTo("PROVISIONAL_DELETION_REFERRAL_REQUEST");
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


    @Test
    void requestOffenderRestrictionCheck() {

        final var request = ArgumentCaptor.forClass(SendMessageRequest.class);

        when(client.sendMessage(request.capture()))
            .thenReturn(new SendMessageResult().withMessageId("message1"));

        eventPusher.requestOffenderRestrictionCheck(OFFENDER_NUMBER, 123L, Set.of(CHILD), "^(regex|1)$");

        assertThat(request.getValue().getQueueUrl()).isEqualTo("queue.url");
        assertThat(request.getValue().getMessageBody()).isEqualTo("{\"offenderIdDisplay\":\"A1234AA\",\"retentionCheckId\":123,\"restrictionCode\":[\"CHILD\"],\"regex\":\"^(regex|1)$\"}");
        assertThat(request.getValue().getMessageAttributes().get("eventType").getStringValue())
            .isEqualTo("DATA_COMPLIANCE_OFFENDER-RESTRICTION-CHECK");
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

    private void mockHmppsService() {
        when(hmppsQueueService.findByQueueId("datacompliancerequest"))
            .thenReturn(hmppsQueue);
        when(hmppsQueue.getSqsClient()).thenReturn(client);
        when(hmppsQueue.getQueueUrl()).thenReturn("queue.url");
    }

    private void invokePostConstruct() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Method postConstruct = DataComplianceAwsEventPusher.class.getDeclaredMethod("initialise", null);
        postConstruct.setAccessible(true);
        postConstruct.invoke(eventPusher);
    }
}

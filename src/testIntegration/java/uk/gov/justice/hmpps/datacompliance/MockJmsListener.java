package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class MockJmsListener {

    public static final String RETENTION_CHECK_ID = "retentionCheckId";
    public static final String EVENT_TYPE = "eventType";

    private final AmazonSQS awsSqsResponseClient;
    private final AmazonSQS awsSqsRequestClient;
    private final String sqsRequestQueueUrl;
    private final ObjectMapper mapper;
    private final Set<Message> messages = new HashSet<>();

    public MockJmsListener(@Qualifier("dataComplianceResponseSqsClient") final AmazonSQS awsSqsResponseClient,
                           @Qualifier("dataComplianceRequestSqsClient") AmazonSQS awsSqsRequestClient,
                           @Qualifier("sqsRequestQueueUrl") String sqsRequestQueueUrl) {
        this.awsSqsResponseClient = awsSqsResponseClient;
        this.awsSqsRequestClient = awsSqsRequestClient;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.sqsRequestQueueUrl = sqsRequestQueueUrl;
    }


    public void verifyMessageReceivedOfEventType(String eventType) {
        verifyMessageReceivedOfEventType(eventType, 1);
    }

    public void verifyNoMessageReceivedOfEventType(String eventType) {
        verifyMessageReceivedOfEventType(eventType, 0);
    }

    public void verifyMessageReceivedOfEventType(String eventType, int count) {
        updateMessages();
        Awaitility.await().until(() -> messages.stream().filter(m -> isMatch(eventType, m)).count() == count);
    }

    public void triggerAdhocDeletion(SendMessageRequest sendMessageRequest) {
        awsSqsResponseClient.sendMessage(sendMessageRequest);
    }

    public void respondToRequestWith(Set<SendMessageRequest> sendMessageRequests) {
        sendMessageRequests.forEach(awsSqsResponseClient::sendMessage);
    }

    private boolean isMatch(String eventType, Message message) {
        return Objects.equals(message.getMessageAttributes().get(EVENT_TYPE).getStringValue(), eventType);
    }

    public void updateMessages(){
        messages.addAll(awsSqsRequestClient.receiveMessage(new ReceiveMessageRequest().withQueueUrl(sqsRequestQueueUrl).withWaitTimeSeconds(10).withMessageAttributeNames(EVENT_TYPE)).getMessages());
    }

    public Long getCheckId(String eventType) {
        return getIdFromPayload(eventType, RETENTION_CHECK_ID);
    }

    public Long getIdFromPayload(String eventType, String value) {
        final String payload = messages.stream().filter(m -> isMatch(eventType, m)).findFirst().orElseThrow(RuntimeException::new).getBody();
        try {
            return mapper.readTree(payload).get(value).asLong();
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }

    public void clearMessages() {
        this.awsSqsRequestClient.receiveMessage(sqsRequestQueueUrl).setMessages(Collections.emptyList());
        this.messages.clear();
    }
}




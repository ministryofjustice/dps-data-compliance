package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class MockJmsListener {

    public static final String RETENTION_CHECK_ID = "retentionCheckId";
    public static final String EVENT_TYPE = "eventType";

    private final AmazonSQS awsSqsResponseClient;
    private final ObjectMapper mapper;
    private final Set<Message<String>> messages = new HashSet<>();

    public MockJmsListener(@Qualifier("dataComplianceResponseSqsClient") final AmazonSQS awsSqsResponseClient) {
        this.awsSqsResponseClient = awsSqsResponseClient;
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @JmsListener(destination = "${data.compliance.request.sqs.queue.name}")
    public void handleEvent(final Message<String> message) {
        messages.add(message);
    }

    public void verifyMessageReceivedOfEventType(String eventType) {
        verifyMessageReceivedOfEventType(eventType, 1);
    }

    public void verifyNoMessageReceivedOfEventType(String eventType) {
        verifyMessageReceivedOfEventType(eventType, 0);
    }

    public void verifyMessageReceivedOfEventType(String eventType, int count) {
        Awaitility.await().until(() -> messages.stream().filter(m -> isMatch(eventType, m)).count() == count);
    }

    public void triggerAdhocDeletion(SendMessageRequest sendMessageRequest) {
        awsSqsResponseClient.sendMessage(sendMessageRequest);
    }

    public void respondToCheckRequestWith(Set<SendMessageRequest> sendMessageRequests) {
        sendMessageRequests.forEach(awsSqsResponseClient::sendMessage);
    }

    private boolean isMatch(String eventType, Message<String> message) {
        return Objects.equals(message.getHeaders().get(EVENT_TYPE), eventType);
    }


    public Long getCheckId(String eventType) {
        return getIdFromPayload(eventType, RETENTION_CHECK_ID);
    }

    public Long getIdFromPayload(String eventType, String value) {
        final String payload = messages.stream().filter(m -> isMatch(eventType, m)).findFirst().orElseThrow(RuntimeException::new).getPayload();
        try {
            return mapper.readTree(payload).get(value).asLong();
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }

    public void clearMessages() {
        this.messages.clear();
    }
}




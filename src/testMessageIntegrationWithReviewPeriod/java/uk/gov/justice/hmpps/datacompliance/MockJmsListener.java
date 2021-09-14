package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.PurgeQueueRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.awaitility.Awaitility;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.sqs.HmppsQueueService;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class MockJmsListener {

    public static final String RETENTION_CHECK_ID = "retentionCheckId";
    public static final String EVENT_TYPE = "eventType";
    private final ObjectMapper mapper;
    private final Set<Message> messages = new HashSet<>();
    private final HmppsQueueService hmppsQueueService;
    private AmazonSQS requestClient;
    private AmazonSQS responseClient;
    private String requestQueueUrl;
    private String responseQueueUrl;

    public MockJmsListener(HmppsQueueService hmppsQueueService) {
        this.hmppsQueueService = hmppsQueueService;
        this.mapper = new ObjectMapper();

    }

    @PostConstruct
    public void setup() {

        final var requestQueue = hmppsQueueService.findByQueueId("datacompliancerequest");
        requestClient = requestQueue.getSqsClient();
        requestQueueUrl = requestQueue.getQueueUrl();

        final var responseQueue = hmppsQueueService.findByQueueId("datacomplianceresponse");
        responseClient = responseQueue.getSqsClient();
        responseQueueUrl = responseQueue.getQueueUrl();

        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
        responseClient.sendMessage(sendMessageRequest);
    }

    public void respondToRequestWith(Set<SendMessageRequest> sendMessageRequests) {
        sendMessageRequests.forEach(responseClient::sendMessage);
    }

    private boolean isMatch(String eventType, Message message) {
        return Objects.equals(message.getMessageAttributes().get(EVENT_TYPE).getStringValue(), eventType);
    }

    public void updateMessages() {
        messages.addAll(requestClient.receiveMessage(new ReceiveMessageRequest().withQueueUrl(requestQueueUrl).withWaitTimeSeconds(10).withMessageAttributeNames(EVENT_TYPE)).getMessages());
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
        requestClient.purgeQueue(new PurgeQueueRequest(requestQueueUrl));
        responseClient.purgeQueue(new PurgeQueueRequest(responseQueueUrl));
        messages.clear();
    }
}




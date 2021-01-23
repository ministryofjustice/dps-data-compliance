package uk.gov.justice.hmpps.datacompliance;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

@Component
public class MockJmsListener{


    private final AmazonSQS awsSqsResponseClient;
    private final ObjectMapper mapper;
    private final Consumer<Set<SendMessageRequest>> messageSender;

    private Set<Configuration> configurationSet = new HashSet<>();
    private Set<Message<String>> messages = new HashSet<>();



    public MockJmsListener(@Qualifier("dataComplianceResponseSqsClient") final AmazonSQS awsSqsResponseClient) {
        this.awsSqsResponseClient = awsSqsResponseClient;
        this.messageSender = (responseSet) -> responseSet.forEach(awsSqsResponseClient::sendMessage);
        this.mapper = new ObjectMapper();
        this.mapper.registerModule(new JavaTimeModule());
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }


    @JmsListener(destination = "${data.compliance.request.sqs.queue.name}")
    public void handleEvent(final Message<String> message) {
        messages.add(message);
        configurationSet.forEach(configuration -> {
            if (isMatch(configuration.getExpectation(), message)) {
                messageSender.accept(configuration.getResponses());
            }
        });

    }

    public Configuration whenReceivedCheckWithEventType(String expectedMessageType) {
        Configuration config = new Configuration(expectedMessageType);
        this.configurationSet.add(config);
        return config;
    }

    public void verifyMessageReceivedOfEventType(String eventType) {
        verifyMessageReceivedOfEventType(eventType, 1);
    }

    public void verifyMessageReceivedOfEventType(String eventType, int count) {
        Awaitility.await().atMost(Duration.ofSeconds(60)).until(() -> messages.stream().filter(m -> isMatch(eventType, m)).count() == count);
    }

    public void respondToCheckRequestWith(SendMessageRequest sendMessageRequest){
        messageSender.accept(Set.of(sendMessageRequest));
    }

    private boolean isMatch(String eventType, Message<String> message) {
        return Objects.equals(message.getHeaders().get("eventType"), eventType);
    }

    public Long getCheckId(String eventType){
      return getIdFromPayload(eventType,"retentionCheckId");
    }


    public Long getIdFromPayload(String eventType, String value){
        final String payload = messages.stream().filter(m -> isMatch(eventType, m)).findFirst().orElseThrow(RuntimeException::new).getPayload();
        try {
            return mapper.readTree(payload).get(value).asLong();
        } catch (JsonProcessingException e) {
            throw new RuntimeException();
        }
    }


}


class Configuration {

    private String expectation;
    private Set<SendMessageRequest> responses;

    public Configuration(String expectation) {
        this.expectation = expectation;
    }

    String getExpectation() {
        return expectation;
    }

    Set<SendMessageRequest> getResponses() {
        return responses;
    }

    public void thenReturn(Set<SendMessageRequest> responses) {
        this.responses = responses;
    }

}



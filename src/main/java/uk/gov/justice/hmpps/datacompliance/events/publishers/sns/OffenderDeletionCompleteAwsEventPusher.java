package uk.gov.justice.hmpps.datacompliance.events.publishers.sns;

import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.MessageAttributeValue;
import com.amazonaws.services.sns.model.PublishRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionComplete;
import uk.gov.justice.hmpps.sqs.HmppsQueueService;

import javax.annotation.PostConstruct;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("{'aws', 'localstack'}.contains('${hmpps.sqs.provider}')")
public class OffenderDeletionCompleteAwsEventPusher implements OffenderDeletionCompleteEventPusher {

    private final HmppsQueueService hmppsQueueService;
    private final ObjectMapper objectMapper;

    private AmazonSNS client;
    private String topicArn;

    @PostConstruct
    private void initialise() {
        final var topic = hmppsQueueService.findByTopicId("datacomplianceevents");
        client = topic.getSnsClient();
        topicArn = topic.getArn();

        log.info("Configured SNS Client to push to topic: {}", topic.getArn());
    }

    @Override
    public void sendEvent(final OffenderDeletionComplete event) {
        client.publish(generateRequest(event));
    }

    private PublishRequest generateRequest(final OffenderDeletionComplete event) {
        return new PublishRequest()
            .withTopicArn(topicArn)
            .withMessageAttributes(Map.of(
                "eventType", stringAttribute("DATA_COMPLIANCE_DELETE-OFFENDER"),
                "contentType", stringAttribute("text/plain;charset=UTF-8")))
            .withMessage(toJson(event));
    }

    private MessageAttributeValue stringAttribute(final String value) {
        return new MessageAttributeValue()
            .withDataType("String")
            .withStringValue(value);
    }

    private String toJson(final OffenderDeletionComplete event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (final Exception e) {
            throw new RuntimeException("Failed to serialise offender deletion complete event", e);
        }
    }
}

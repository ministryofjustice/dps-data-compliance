package uk.gov.justice.hmpps.datacompliance.services.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionEvent;

import java.util.Map;

import static uk.gov.justice.hmpps.datacompliance.utils.Exceptions.propagateAnyError;

@Slf4j
@Component
@ConditionalOnProperty(name = "sns.provider")
public class OffenderDeletionAwsEventPusher implements OffenderDeletionEventPusher {

    private final ObjectMapper objectMapper;
    private final SnsAsyncClient snsClient;
    private final String topicArn;

    public OffenderDeletionAwsEventPusher(final SnsAsyncClient snsClient,
                                          @Value("${sns.topic.arn}") final String topicArn,
                                          final ObjectMapper objectMapper) {

        log.info("Configured to push offender deletion events to SNS topic: {}", topicArn);

        this.objectMapper = objectMapper;
        this.snsClient = snsClient;
        this.topicArn = topicArn;
    }

    @Override
    public void sendEvent(final String offenderDisplayId) {

        log.debug("Sending request for offender deletion: {}", offenderDisplayId);

        snsClient.publish(generateRequest(offenderDisplayId))
                .whenComplete((response, throwable) -> {
                    if (response != null) log.debug("Pushing event for: {} succeeded: {}", offenderDisplayId, response);
                    if (throwable != null) log.error("Pushing event for: {} failed:", offenderDisplayId, throwable);
                });
    }

    private PublishRequest generateRequest(final String offenderDisplayId) {

        final OffenderDeletionEvent event = new OffenderDeletionEvent(offenderDisplayId);

        return PublishRequest.builder()
                .topicArn(topicArn)
                .messageAttributes(Map.of(
                        "eventType", stringAttribute("DATA_COMPLIANCE_DELETE-OFFENDER"),
                        "contentType", stringAttribute("text/plain;charset=UTF-8")))
                .message(propagateAnyError(() -> objectMapper.writeValueAsString(event)))
                .build();
    }

    private MessageAttributeValue stringAttribute(final String value) {
        return MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(value)
                .build();
    }
}

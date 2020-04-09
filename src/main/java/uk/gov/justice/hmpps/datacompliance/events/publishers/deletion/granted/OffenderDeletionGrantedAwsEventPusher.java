package uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.MessageAttributeValue;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionGrantedEvent;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("{'aws', 'localstack'}.contains('${outbound.deletion.sqs.provider}')")
public class OffenderDeletionGrantedAwsEventPusher implements OffenderDeletionGrantedEventPusher {

    private final ObjectMapper objectMapper;
    private final AmazonSQS sqsClient;
    private final String queueUrl;

    public OffenderDeletionGrantedAwsEventPusher(
            @Autowired @Qualifier("outboundDeletionSqsClient") final AmazonSQS sqsClient,
            @Value("${outbound.deletion.sqs.queue.url}") final String queueUrl,
            final ObjectMapper objectMapper) {

        log.info("Configured to push offender deletion granted events to SQS queue: {}", queueUrl);

        this.objectMapper = objectMapper;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void grantDeletion(final OffenderNumber offenderNo, final Long referralId) {

        log.debug("Sending offender deletion granted event for: {}", offenderNo);

        sqsClient.sendMessage(generateDeletionGrantedRequest(offenderNo, referralId));
    }

    private SendMessageRequest generateDeletionGrantedRequest(final OffenderNumber offenderNo, final Long referralId) {
        return new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageAttributes(Map.of(
                        "eventType", stringAttribute("DATA_COMPLIANCE_OFFENDER-DELETION-GRANTED"),
                        "contentType", stringAttribute("application/json;charset=UTF-8")))
                .withMessageBody(toJson(new OffenderDeletionGrantedEvent(offenderNo.getOffenderNumber(), referralId)));
    }

    private MessageAttributeValue stringAttribute(final String value) {
        return new MessageAttributeValue()
                .withDataType("String")
                .withStringValue(value);
    }

    private String toJson(final Object event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}

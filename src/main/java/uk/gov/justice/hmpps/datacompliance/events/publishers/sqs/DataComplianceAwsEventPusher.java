package uk.gov.justice.hmpps.datacompliance.events.publishers.sqs;

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
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.DataDuplicateCheck;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.FreeTextSearchRequest;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionGranted;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("{'aws', 'localstack'}.contains('${data.compliance.request.sqs.provider}')")
public class DataComplianceAwsEventPusher implements DataComplianceEventPusher {

    private static final String OFFENDER_DELETION_GRANTED = "DATA_COMPLIANCE_OFFENDER-DELETION-GRANTED";
    private static final String DATA_DUPLICATE_ID_CHECK = "DATA_COMPLIANCE_DATA-DUPLICATE-ID-CHECK";
    private static final String DATA_DUPLICATE_DB_CHECK = "DATA_COMPLIANCE_DATA-DUPLICATE-DB-CHECK";
    private static final String FREE_TEXT_MORATORIUM_CHECK = "DATA_COMPLIANCE_FREE-TEXT-MORATORIUM-CHECK";

    private final ObjectMapper objectMapper;
    private final AmazonSQS sqsClient;
    private final String queueUrl;

    public DataComplianceAwsEventPusher(
            @Autowired @Qualifier("dataComplianceRequestSqsClient") final AmazonSQS sqsClient,
            @Value("${data.compliance.request.sqs.queue.url}") final String queueUrl,
            final ObjectMapper objectMapper) {

        log.info("Configured to push offender deletion granted events to SQS queue: {}", queueUrl);

        this.objectMapper = objectMapper;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void grantDeletion(final OffenderNumber offenderNo, final Long referralId) {

        log.debug("Sending grant deletion event for: '{}/{}'", offenderNo.getOffenderNumber(), referralId);

        sqsClient.sendMessage(generateRequest(OFFENDER_DELETION_GRANTED,
                new OffenderDeletionGranted(offenderNo.getOffenderNumber(), referralId)));
    }

    @Override
    public void requestIdDataDuplicateCheck(final OffenderNumber offenderNo, final Long retentionCheckId) {

        log.debug("Requesting ID data duplicate check for: '{}/{}'", offenderNo.getOffenderNumber(), retentionCheckId);

        sqsClient.sendMessage(generateRequest(DATA_DUPLICATE_ID_CHECK,
                new DataDuplicateCheck(offenderNo.getOffenderNumber(), retentionCheckId)));
    }

    @Override
    public void requestDatabaseDataDuplicateCheck(final OffenderNumber offenderNo, Long retentionCheckId) {

        log.debug("Requesting data duplicate database check for: '{}/{}'", offenderNo.getOffenderNumber(), retentionCheckId);

        sqsClient.sendMessage(generateRequest(DATA_DUPLICATE_DB_CHECK,
                new DataDuplicateCheck(offenderNo.getOffenderNumber(), retentionCheckId)));
    }

    @Override
    public void requestFreeTextMoratoriumCheck(final OffenderNumber offenderNo,
                                               final Long retentionCheckId,
                                               final String regex) {

        log.debug("Requesting free text moratorium check for: '{}/{}'", offenderNo.getOffenderNumber(), retentionCheckId);

        sqsClient.sendMessage(generateRequest(FREE_TEXT_MORATORIUM_CHECK,
                new FreeTextSearchRequest(offenderNo.getOffenderNumber(), retentionCheckId, regex)));
    }

    private SendMessageRequest generateRequest(final String eventType, final Object messageBody) {
        return new SendMessageRequest()
                .withQueueUrl(queueUrl)
                .withMessageAttributes(Map.of(
                        "eventType", stringAttribute(eventType),
                        "contentType", stringAttribute("application/json;charset=UTF-8")))
                .withMessageBody(toJson(messageBody));
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

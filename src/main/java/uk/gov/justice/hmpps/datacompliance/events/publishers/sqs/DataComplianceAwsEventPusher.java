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
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionGrant;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionReferralRequest;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.AdHocReferralRequest;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.DataDuplicateCheck;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.FreeTextSearchRequest;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionGranted;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.ReferralRequest;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("{'aws', 'localstack', 'embedded-localstack'}.contains('${data.compliance.request.sqs.provider}')")
public class DataComplianceAwsEventPusher implements DataComplianceEventPusher {

    private static final String REFERRAL_REQUEST = "DATA_COMPLIANCE_REFERRAL-REQUEST";
    private static final String AD_HOC_REFERRAL_REQUEST = "DATA_COMPLIANCE_AD-HOC-REFERRAL-REQUEST";
    private static final String OFFENDER_DELETION_GRANTED = "DATA_COMPLIANCE_OFFENDER-DELETION-GRANTED";
    private static final String DATA_DUPLICATE_ID_CHECK = "DATA_COMPLIANCE_DATA-DUPLICATE-ID-CHECK";
    private static final String DATA_DUPLICATE_DB_CHECK = "DATA_COMPLIANCE_DATA-DUPLICATE-DB-CHECK";
    private static final String FREE_TEXT_MORATORIUM_CHECK = "DATA_COMPLIANCE_FREE-TEXT-MORATORIUM-CHECK";

    private final ObjectMapper objectMapper;
    private final AmazonSQS sqsClient;
    private final String queueUrl;

    public DataComplianceAwsEventPusher(
            @Autowired @Qualifier("dataComplianceRequestSqsClient") final AmazonSQS sqsClient,
            @Value("${data.compliance.request.sqs.queue.name}") final String name,
            final ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;
        this.sqsClient = sqsClient;
        this.queueUrl = sqsClient.getQueueUrl(name).getQueueUrl();

        log.info("Configured to push offender deletion granted events to SQS queue: {}", queueUrl);
    }

    @Override
    public void requestReferral(final OffenderDeletionReferralRequest request) {

        log.debug("Requesting deletion referral: {}", request);

        sqsClient.sendMessage(generateRequest(REFERRAL_REQUEST,
                ReferralRequest.builder()
                        .batchId(request.getBatchId())
                        .dueForDeletionWindowStart(request.getDueForDeletionWindowStart())
                        .dueForDeletionWindowEnd(request.getDueForDeletionWindowEnd())
                        .limit(request.getLimit())
                        .build()));
    }

    @Override
    public void requestAdHocReferral(final OffenderNumber offenderNo, final Long batchId) {

        log.debug("Requesting ad hoc deletion referral for offender: '{}' and batch: '{}'",
                offenderNo.getOffenderNumber(), batchId);

        sqsClient.sendMessage(generateRequest(AD_HOC_REFERRAL_REQUEST,
                new AdHocReferralRequest(offenderNo.getOffenderNumber(), batchId)));
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
                                               final List<String> regex) {

        log.debug("Requesting free text moratorium check for: '{}/{}'", offenderNo.getOffenderNumber(), retentionCheckId);

        sqsClient.sendMessage(generateRequest(FREE_TEXT_MORATORIUM_CHECK,
                new FreeTextSearchRequest(offenderNo.getOffenderNumber(), retentionCheckId, regex)));
    }

    @Override
    public void grantDeletion(final OffenderDeletionGrant offenderDeletionGrant) {

        log.debug("Sending grant deletion event for: '{}/{}'",
                offenderDeletionGrant.getOffenderNumber().getOffenderNumber(), offenderDeletionGrant.getReferralId());

        sqsClient.sendMessage(generateRequest(OFFENDER_DELETION_GRANTED, OffenderDeletionGranted.builder()
                .offenderIdDisplay(offenderDeletionGrant.getOffenderNumber().getOffenderNumber())
                .referralId(offenderDeletionGrant.getReferralId())
                .offenderIds(offenderDeletionGrant.getOffenderIds())
                .offenderBookIds(offenderDeletionGrant.getOffenderBookIds())
                .build()));
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

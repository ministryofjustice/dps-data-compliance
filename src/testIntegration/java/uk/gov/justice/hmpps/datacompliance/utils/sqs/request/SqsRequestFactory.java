package uk.gov.justice.hmpps.datacompliance.utils.sqs.request;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import lombok.Getter;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.utils.sqs.EventType.Request;
import uk.gov.justice.hmpps.datacompliance.utils.sqs.Factory;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;


@Getter
public class SqsRequestFactory extends Factory {


    public static SendMessageRequest forDeletionGranted(String queueUrl, String offenderIdDisplay, Long referralId) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Request.OFFENDER_DELETION_GRANTED),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(OffenderDeletionGranted.builder()
                .offenderIdDisplay(offenderIdDisplay)
                .referralId(referralId)
                .build()));
    }

    public static SendMessageRequest forReferral(String queueUrl, OffenderDeletionBatch batch) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Request.REFERRAL_REQUEST),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(ReferralRequest.builder()
                .batchId(batch.getBatchId())
                .dueForDeletionWindowStart(batch.getWindowStartDateTime().toLocalDate())
                .dueForDeletionWindowEnd(batch.getWindowStartDateTime().toLocalDate())
                .build()));
    }

}


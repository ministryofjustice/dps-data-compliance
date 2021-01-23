package uk.gov.justice.hmpps.datacompliance.utils.sqs.response;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import uk.gov.justice.hmpps.datacompliance.utils.sqs.EventType.Response;
import uk.gov.justice.hmpps.datacompliance.utils.sqs.Factory;

import java.time.LocalDate;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class SqsResponseFactory extends Factory {

    public static SendMessageRequest forOffenderPendingDeletion(String queueUrl, Long batchId, String offenderIdDisplay) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Response.OFFENDER_PENDING_DELETION_EVENT),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(OffenderPendingDeletion.builder()
                .batchId(batchId)
                .offenderIdDisplay(offenderIdDisplay)
                .birthDate(LocalDate.now().minusYears(30))
                .firstName("someFirstName")
                .middleName("someMiddleName")
                .lastName("someLastName")
                .build()));
    }


    public static SendMessageRequest forDataDuplicateIdResult(String queueUrl, String offenderIdDisplay, Long checkId) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Response.DATA_DUPLICATE_ID_RESULT),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(DataDuplicateResult.builder()
                .offenderIdDisplay(offenderIdDisplay)
                .retentionCheckId(checkId)
                .build()));
    }


    public static SendMessageRequest forDataDuplicateDbResult(String queueUrl, String offenderIdDisplay, Long checkId) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Response.DATA_DUPLICATE_DB_RESULT),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(DataDuplicateResult.builder()
                .offenderIdDisplay(offenderIdDisplay)
                .retentionCheckId(checkId)
                .build()));
    }


    public static SendMessageRequest forFreeTextSearchResult(String queueUrl, String offenderIdDisplay, Long checkId) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Response.FREE_TEXT_MORATORIUM_RESULT),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(FreeTextSearchResult.builder()
                .offenderIdDisplay(offenderIdDisplay)
                .retentionCheckId(checkId)
                .build()));
    }

    public static SendMessageRequest forPendingDeletionReferralComplete(String queueUrl, Long batchId, Long numOfReferred, Long totalInWindow) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Response.OFFENDER_PENDING_DELETION_REFERRAL_COMPLETE_EVENT),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(OffenderPendingDeletionReferralComplete.builder()
                .batchId(batchId)
                .numberReferred(numOfReferred)
                .totalInWindow(totalInWindow)
                .build()));
    }


    public static SendMessageRequest forOffenderDeletionCompleteResult(String queueUrl, Long referralId, String offenderIdDisplay) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Response.OFFENDER_DELETION_COMPLETE_EVENT),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(OffenderDeletionComplete.builder()
                .referralId(referralId)
                .offenderIdDisplay(offenderIdDisplay)
                .build()));
    }



}

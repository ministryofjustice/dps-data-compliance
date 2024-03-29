package uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response;

import com.amazonaws.services.sqs.model.SendMessageRequest;
import uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.QueueFactory;
import uk.gov.justice.hmpps.datacompliance.utils.queue.sqs.response.DeceasedOffenderDeletionResult.DeceasedOffender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class SqsResponseQueueFactory extends QueueFactory {

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
                .agencyLocationId("someAgencyLocationId")
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


    public static SendMessageRequest forOffenderRestrictionResult(String queueUrl, String offenderIdDisplay, Long checkId, boolean isRestricted) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Response.OFFENDER_RESTRICTION_RESULT),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(OffenderRestrictionResult.builder()
                .offenderIdDisplay(offenderIdDisplay)
                .retentionCheckId(checkId)
                .restricted(isRestricted)
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


    public static SendMessageRequest forAdHocDeletionEvent(String queueUrl, String offenderDisplayId) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Response.ADHOC_OFFENDER_DELETION_EVENT),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(AdHocOffenderDeletion.builder()
                .offenderIdDisplay(offenderDisplayId)
                .reason("some reason for requesting ad hoc deletion")
                .build()));
    }

    public static SendMessageRequest forDeceasedOffenderDeletionResult(String queueUrl, Long batchId, DeceasedOffender deceasedOffender) {
        return new SendMessageRequest()
            .withQueueUrl(queueUrl)
            .withMessageAttributes(Map.of(
                EVENT_TYPE, stringAttribute(Response.DECEASED_OFFENDER_DELETION_RESULT),
                CONTENT_TYPE, stringAttribute(APPLICATION_JSON_VALUE)))
            .withMessageBody(asJson(DeceasedOffenderDeletionResult.builder()
                .batchId(batchId)
                .deceasedOffender(deceasedOffender)
                .build()));
    }

    public static DeceasedOffender getDeceasedOffender(String offenderIdDisplay) {
        return DeceasedOffender.builder()
            .offenderIdDisplay(offenderIdDisplay)
            .firstName("someFirstName")
            .middleName("someMiddleName")
            .lastName("someLastName")
            .agencyLocationId("someAgencyLocationId")
            .birthDate(LocalDate.now().minusYears(30))
            .deceasedDate(LocalDate.now().minusYears(1))
            .deletionDateTime(LocalDateTime.now().minusMinutes(1))
            .build();
    }
}

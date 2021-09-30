package uk.gov.justice.hmpps.datacompliance.events.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.AdHocOffenderDeletion;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DataDuplicateResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DeceasedOffenderDeletionResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.FreeTextSearchResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionComplete;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralComplete;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderRestrictionResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.ProvisionalDeletionReferralResult;
import uk.gov.justice.hmpps.datacompliance.services.deletion.DeceasedDeletionService;
import uk.gov.justice.hmpps.datacompliance.services.deletion.DeletionService;
import uk.gov.justice.hmpps.datacompliance.services.referral.ReferralService;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;

import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.DATABASE;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.ID;

@Slf4j
@Service
@ConditionalOnExpression("{'aws', 'localstack', 'embedded-localstack'}.contains('${data.compliance.response.sqs.provider}')")
public class DataComplianceEventListener {

    private static final String ADHOC_OFFENDER_DELETION_EVENT = "DATA_COMPLIANCE_AD-HOC-OFFENDER-DELETION";
    private static final String OFFENDER_PENDING_DELETION_EVENT = "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION";
    private static final String OFFENDER_PROVISIONAL_DELETION_REFERRAL_EVENT = "DATA_COMPLIANCE_OFFENDER_PROVISIONAL_DELETION_REFERRAL";
    private static final String OFFENDER_PENDING_DELETION_REFERRAL_COMPLETE_EVENT = "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION-REFERRAL-COMPLETE";
    private static final String OFFENDER_DELETION_COMPLETE_EVENT = "DATA_COMPLIANCE_OFFENDER-DELETION-COMPLETE";
    private static final String DATA_DUPLICATE_ID_RESULT = "DATA_COMPLIANCE_DATA-DUPLICATE-ID-RESULT";
    private static final String DATA_DUPLICATE_DB_RESULT = "DATA_COMPLIANCE_DATA-DUPLICATE-DB-RESULT";
    private static final String FREE_TEXT_MORATORIUM_RESULT = "DATA_COMPLIANCE_FREE-TEXT-MORATORIUM-RESULT";
    private static final String OFFENDER_RESTRICTION_RESULT= "DATA_COMPLIANCE_OFFENDER-RESTRICTION-RESULT";
    private static final String DECEASED_OFFENDER_DELETION_RESULT = "DATA_COMPLIANCE_DECEASED-OFFENDER-DELETION-RESULT";


    private final Map<String, MessageHandler> messageHandlers = Map.of(
            ADHOC_OFFENDER_DELETION_EVENT, this::handleAdHocDeletion,
            OFFENDER_PENDING_DELETION_EVENT, this::handlePendingDeletionReferral,
            OFFENDER_PROVISIONAL_DELETION_REFERRAL_EVENT, this::handleProvisionalDeletionReferralResult,
            OFFENDER_PENDING_DELETION_REFERRAL_COMPLETE_EVENT, this::handleReferralComplete,
            OFFENDER_DELETION_COMPLETE_EVENT, this::handleDeletionComplete,
            DATA_DUPLICATE_ID_RESULT, this::handleDataDuplicateIdResult,
            DATA_DUPLICATE_DB_RESULT, this::handleDataDuplicateDbResult,
            FREE_TEXT_MORATORIUM_RESULT, this::handleFreeTextSearchResult,
            OFFENDER_RESTRICTION_RESULT, this::handleOffenderRestrictionResult,
            DECEASED_OFFENDER_DELETION_RESULT, this::handleDeceasedOffenderDeletionResult
        );

    private final ObjectMapper objectMapper;
    private final ReferralService referralService;
    private final RetentionService retentionService;
    private final DeletionService deletionService;
    private final DeceasedDeletionService deceasedDeletionService;

    public DataComplianceEventListener(final ObjectMapper objectMapper,
                                       final ReferralService referralService,
                                       final RetentionService retentionService,
                                       final DeletionService deletionService,
                                       final DeceasedDeletionService deceasedDeletionService) {

        log.info("Configured to listen to Offender Deletion events");

        this.objectMapper = objectMapper;
        this.referralService = referralService;
        this.retentionService = retentionService;
        this.deletionService = deletionService;
        this.deceasedDeletionService = deceasedDeletionService;
    }

    @JmsListener(destination = "${data.compliance.response.sqs.queue.name}")
    public void handleEvent(final Message<String> message) {

        final var eventType = getEventType(message.getHeaders());

        log.debug("Handling incoming data compliance event of type: {}", eventType);

        messageHandlers.get(eventType).handle(message);
    }

    private String getEventType(final MessageHeaders messageHeaders) {

        final var eventType = messageHeaders.get("eventType", String.class);

        checkNotNull(eventType, "Message event type not found");
        checkState(messageHandlers.containsKey(eventType),
                "Unexpected message event type: '%s', expecting one of: %s", eventType, messageHandlers.keySet());

        return eventType;
    }

    private void handleDeletionComplete(final Message<String> message) {
        deletionService.handleDeletionComplete(
                parseEvent(message.getPayload(), OffenderDeletionComplete.class));
    }

    private void handleReferralComplete(final Message<String> message) {
        referralService.handleReferralComplete(
                parseEvent(message.getPayload(), OffenderPendingDeletionReferralComplete.class));
    }

    private void handlePendingDeletionReferral(final Message<String> message) {
        referralService.handlePendingDeletionReferral(
                parseEvent(message.getPayload(), OffenderPendingDeletion.class));
    }

    private void handleProvisionalDeletionReferralResult(final Message<String> message) {
        referralService.handleProvisionalDeletionReferralResult(
            parseEvent(message.getPayload(), ProvisionalDeletionReferralResult.class));
    }

    private void handleAdHocDeletion(final Message<String> message) {
        referralService.handleAdHocDeletion(
                parseEvent(message.getPayload(), AdHocOffenderDeletion.class));
    }

    private void handleDataDuplicateIdResult(final Message<String> message) {
        retentionService.handleDataDuplicateResult(
                parseEvent(message.getPayload(), DataDuplicateResult.class), ID);
    }

    private void handleDataDuplicateDbResult(final Message<String> message) {
        retentionService.handleDataDuplicateResult(
                parseEvent(message.getPayload(), DataDuplicateResult.class), DATABASE);
    }

    private void handleFreeTextSearchResult(final Message<String> message) {
        retentionService.handleFreeTextSearchResult(
                parseEvent(message.getPayload(), FreeTextSearchResult.class));
    }

    private void handleOffenderRestrictionResult(final Message<String> message) {
        retentionService.handleOffenderRestrictionResult(
            parseEvent(message.getPayload(), OffenderRestrictionResult.class));
    }

    private void handleDeceasedOffenderDeletionResult(final Message<String> message) {
        deceasedDeletionService.handleDeceasedOffenderDeletionResult(
            parseEvent(message.getPayload(), DeceasedOffenderDeletionResult.class));
    }

    private <T> T parseEvent(final String requestJson, final Class<T> eventType) {
        try {
            return objectMapper.readValue(requestJson, eventType);
        } catch (final IOException e) {
            throw new RuntimeException("Failed to parse request: " + requestJson, e);
        }
    }

    @FunctionalInterface
    private interface MessageHandler {
        void handle(Message<String> message);
    }
}

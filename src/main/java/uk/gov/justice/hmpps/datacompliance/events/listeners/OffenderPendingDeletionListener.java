package uk.gov.justice.hmpps.datacompliance.events.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import uk.gov.justice.hmpps.datacompliance.events.dto.OffenderPendingDeletionEvent;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

@Slf4j
@Service
@ConditionalOnExpression("{'aws', 'localstack'}.contains('${inbound.referral.sqs.provider}')")
public class OffenderPendingDeletionListener {

    private static final String OFFENDER_PENDING_DELETION_EVENT = "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION";
    private static final String OFFENDER_PENDING_DELETION_COMPLETE_EVENT = "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION-COMPLETE";
    private static final List<String> EXPECTED_EVENT_TYPES = List.of(
            OFFENDER_PENDING_DELETION_EVENT,
            OFFENDER_PENDING_DELETION_COMPLETE_EVENT);

    private final ObjectMapper objectMapper;

    public OffenderPendingDeletionListener(final ObjectMapper objectMapper) {

        log.info("Configured to listen to Offender Pending Deletion events");

        this.objectMapper = objectMapper;
    }

    @JmsListener(destination = "${inbound.referral.sqs.queue.name}")
    public void handleOffenderPendingDeletionReferral(final Message<String> message) {

        log.debug("Handling incoming offender pending deletion referral: {}", message);

        final var eventType = getEventType(message.getHeaders());

        if (OFFENDER_PENDING_DELETION_EVENT.equals(eventType)) {
            // TODO GDPR-51 Implement pipeline of offender checks
            log.warn("Not yet implemented pipeline of offender checks, ignoring referral for offender: '{}'",
                    getOffenderIdDisplay(message.getPayload()));
        }
    }

    private String getEventType(final MessageHeaders messageHeaders) {

        final var eventType = messageHeaders.get("eventType", String.class);

        checkNotNull(eventType, "Message event type not found");
        checkState(EXPECTED_EVENT_TYPES.contains(eventType),
                "Unexpected message event type: '%s', expecting one of: %s", eventType, EXPECTED_EVENT_TYPES);

        return eventType;
    }

    private String getOffenderIdDisplay(final String messageBody) {

        final OffenderPendingDeletionEvent event = parseOffenderPendingDeletionEvent(messageBody);

        checkState(isNotEmpty(event.getOffenderIdDisplay()), "No offender specified in request: %s", messageBody);

        return event.getOffenderIdDisplay();
    }

    private OffenderPendingDeletionEvent parseOffenderPendingDeletionEvent(final String requestJson) {
        try {
            return objectMapper.readValue(requestJson, OffenderPendingDeletionEvent.class);

        } catch (final IOException e) {
            throw new RuntimeException("Failed to parse request: " + requestJson, e);
        }
    }
}

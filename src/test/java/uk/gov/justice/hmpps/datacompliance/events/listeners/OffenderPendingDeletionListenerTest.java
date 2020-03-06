package uk.gov.justice.hmpps.datacompliance.events.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OffenderPendingDeletionListenerTest {

    private OffenderPendingDeletionListener listener;

    @BeforeEach
    void setUp() {
        listener = new OffenderPendingDeletionListener(new ObjectMapper());
    }

    @Test
    void handleOffenderDeletionEventThrowsIfMessageAttributesNotPresent() {
        assertThatThrownBy(() -> handleMessage("{\"offenderIdDisplay\":\"A1234AA\"}", Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Message event type not found");
    }

    @Test
    void handleOffenderDeletionEventThrowsIfEventTypeUnexpected() {
        assertThatThrownBy(() -> handleMessage("{\"offenderIdDisplay\":\"A1234AA\"}", Map.of("eventType", "UNEXPECTED!")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unexpected message event type: 'UNEXPECTED!', expecting one of: [DATA_COMPLIANCE_OFFENDER-PENDING-DELETION, DATA_COMPLIANCE_OFFENDER-PENDING-DELETION-COMPLETE]");
    }

    @Test
    void handleOffenderDeletionEventThrowsIfMessageNotPresent() {
        assertThatThrownBy(() -> handleMessage(null, Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("argument \"content\" is null");
    }

    @Test
    void handleOffenderDeletionEventThrowsIfMessageUnparsable() {
        assertThatThrownBy(() -> handleMessage("BAD MESSAGE!", Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to parse request");
    }

    @Test
    void handleOffenderDeletionEventThrowsIfOffenderIdDisplayEmpty() {
        assertThatThrownBy(() -> handleMessage("{\"offenderIdDisplay\":\"\"}", Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No offender specified in request");
    }

    private void handleMessage(final String payload, final Map<String, Object> headers) {
        listener.handleOffenderPendingDeletionReferral(mockMessage(payload, headers));
    }

    @SuppressWarnings("unchecked")
    private Message<String> mockMessage(final String payload, final Map<String, Object> headers) {
        final var message = mock(Message.class);
        when(message.getHeaders()).thenReturn(new MessageHeaders(headers));
        lenient().when(message.getPayload()).thenReturn(payload);
        return message;
    }
}
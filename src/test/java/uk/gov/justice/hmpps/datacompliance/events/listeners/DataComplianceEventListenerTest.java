package uk.gov.justice.hmpps.datacompliance.events.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.OffenderBooking;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.OffenderWithBookings;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.services.deletion.DeletionService;
import uk.gov.justice.hmpps.datacompliance.services.referral.ReferralService;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataComplianceEventListenerTest {

    @Mock
    private ReferralService referralService;

    @Mock
    private DeletionService deletionService;

    private DataComplianceEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new DataComplianceEventListener(new ObjectMapper(), referralService, deletionService);
    }

    @Test
    void handlePendingDeletionEvent() {
        handleMessage(
                "{" +
                        "\"offenderIdDisplay\":\"A1234AA\"," +
                        "\"firstName\":\"Bob\"," +
                        "\"middleName\":\"Middle\"," +
                        "\"lastName\":\"Jones\"," +
                        "\"birthDate\":\"1990-01-02\"," +
                        "\"offenders\":[{\"offenderId\":123,\"bookings\":[{\"offenderBookId\":321}]}]" +
                "}",
                Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION"));

        verify(referralService).handlePendingDeletionReferral(OffenderPendingDeletionEvent.builder()
                .offenderIdDisplay("A1234AA")
                .firstName("Bob")
                .middleName("Middle")
                .lastName("Jones")
                .birthDate(LocalDate.of(1990, 1, 2))
                .offender(OffenderWithBookings.builder()
                        .offenderId(123L)
                        .offenderBooking(new OffenderBooking(321L))
                        .build())
                .build());
    }

    @Test
    void handleReferralComplete() {
        handleMessage("{\"batchId\":123}",
                Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION-REFERRAL-COMPLETE"));

        verify(referralService).handleReferralComplete(new OffenderPendingDeletionReferralCompleteEvent(123L));
    }

    @Test
    void handleDeletionComplete() {
        handleMessage("{\"offenderIdDisplay\":\"A1234AA\",\"referralId\":123}",
                Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-DELETION-COMPLETE"));

        verify(deletionService).handleDeletionComplete(new OffenderDeletionCompleteEvent("A1234AA", 123L));
    }

    @Test
    void handleOffenderDeletionEventThrowsIfMessageAttributesNotPresent() {
        assertThatThrownBy(() -> handleMessage("{\"offenderIdDisplay\":\"A1233AA\"}", Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Message event type not found");
    }

    @Test
    void handleOffenderDeletionEventThrowsIfEventTypeUnexpected() {
        assertThatThrownBy(() -> handleMessage("{\"offenderIdDisplay\":\"A1234AA\"}", Map.of("eventType", "UNEXPECTED!")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected message event type: 'UNEXPECTED!'");
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

    private void handleMessage(final String payload, final Map<String, Object> headers) {
        listener.handleEvent(mockMessage(payload, headers));
    }

    @SuppressWarnings("unchecked")
    private Message<String> mockMessage(final String payload, final Map<String, Object> headers) {
        final var message = mock(Message.class);
        when(message.getHeaders()).thenReturn(new MessageHeaders(headers));
        lenient().when(message.getPayload()).thenReturn(payload);
        return message;
    }
}
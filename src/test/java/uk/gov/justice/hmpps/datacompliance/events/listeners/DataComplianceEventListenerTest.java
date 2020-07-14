package uk.gov.justice.hmpps.datacompliance.events.listeners;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.AdHocOffenderDeletion;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DataDuplicateResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.FreeTextSearchResult;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionComplete;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion.OffenderBooking;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion.OffenderAlias;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralComplete;
import uk.gov.justice.hmpps.datacompliance.services.deletion.DeletionService;
import uk.gov.justice.hmpps.datacompliance.services.referral.ReferralService;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.DATABASE;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.ID;

@ExtendWith(MockitoExtension.class)
class DataComplianceEventListenerTest {

    @Mock
    private ReferralService referralService;

    @Mock
    private DeletionService deletionService;

    @Mock
    private RetentionService retentionService;

    private DataComplianceEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new DataComplianceEventListener(new ObjectMapper(), referralService, retentionService, deletionService);
    }

    @Test
    void handleAdHocOffenderDeletionEvent() {
        handleMessage("{\"offenderIdDisplay\":\"A1234AA\",\"reason\":\"Some reason\"}",
                Map.of("eventType", "DATA_COMPLIANCE_AD-HOC-OFFENDER-DELETION"));

        verify(referralService).handleAdHocDeletion(new AdHocOffenderDeletion("A1234AA", "Some reason"));
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
                        "\"offenderAliases\":[{" +
                            "\"offenderId\":123," +
                            "\"bookings\":[{\"offenderBookId\":321,\"offenceCodes\":[\"offence1\"]}]" +
                        "}]" +
                "}",
                Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION"));

        verify(referralService).handlePendingDeletionReferral(OffenderPendingDeletion.builder()
                .offenderIdDisplay("A1234AA")
                .firstName("Bob")
                .middleName("Middle")
                .lastName("Jones")
                .birthDate(LocalDate.of(1990, 1, 2))
                .offenderAlias(OffenderAlias.builder()
                        .offenderId(123L)
                        .offenderBooking(OffenderBooking.builder()
                                .offenderBookId(321L)
                                .offenceCode("offence1")
                                .build())
                        .build())
                .build());
    }

    @Test
    void handleReferralComplete() {
        handleMessage("{\"batchId\":123,\"numberReferred\":4,\"totalInWindow\":5}",
                Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION-REFERRAL-COMPLETE"));

        verify(referralService).handleReferralComplete(new OffenderPendingDeletionReferralComplete(123L, 4L, 5L));
    }

    @Test
    void handleDeletionComplete() {
        handleMessage("{\"offenderIdDisplay\":\"A1234AA\",\"referralId\":123}",
                Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-DELETION-COMPLETE"));

        verify(deletionService).handleDeletionComplete(new OffenderDeletionComplete("A1234AA", 123L));
    }

    @Test
    void handleDataDuplicateIdResult() {
        handleMessage("{\"offenderIdDisplay\":\"A1234AA\",\"retentionCheckId\":123,\"duplicateOffenders\":[\"B1234BB\"]}",
                Map.of("eventType", "DATA_COMPLIANCE_DATA-DUPLICATE-ID-RESULT"));

        verify(retentionService).handleDataDuplicateResult(new DataDuplicateResult("A1234AA", 123L, List.of("B1234BB")), ID);
    }

    @Test
    void handleDataDuplicateDbResult() {
        handleMessage("{\"offenderIdDisplay\":\"A1234AA\",\"retentionCheckId\":123,\"duplicateOffenders\":[\"B1234BB\"]}",
                Map.of("eventType", "DATA_COMPLIANCE_DATA-DUPLICATE-DB-RESULT"));

        verify(retentionService).handleDataDuplicateResult(new DataDuplicateResult("A1234AA", 123L, List.of("B1234BB")), DATABASE);
    }

    @Test
    void handleFreeTextSearchResult() {
        handleMessage("{\"offenderIdDisplay\":\"A1234AA\",\"retentionCheckId\":123,\"matchingTables\":[\"TABLE1\"]}",
                Map.of("eventType", "DATA_COMPLIANCE_FREE-TEXT-MORATORIUM-RESULT"));

        verify(retentionService).handleFreeTextSearchResult(new FreeTextSearchResult("A1234AA", 123L, List.of("TABLE1")));
    }

    @Test
    void handleEventThrowsIfMessageAttributesNotPresent() {
        assertThatThrownBy(() -> handleMessage("{\"offenderIdDisplay\":\"A1233AA\"}", Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("Message event type not found");
    }

    @Test
    void handleEventThrowsIfEventTypeUnexpected() {
        assertThatThrownBy(() -> handleMessage("{\"offenderIdDisplay\":\"A1234AA\"}", Map.of("eventType", "UNEXPECTED!")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unexpected message event type: 'UNEXPECTED!'");
    }

    @Test
    void handleEventThrowsIfMessageNotPresent() {
        assertThatThrownBy(() -> handleMessage(null, Map.of("eventType", "DATA_COMPLIANCE_OFFENDER-PENDING-DELETION")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("argument \"content\" is null");
    }

    @Test
    void handleEventThrowsIfMessageUnparsable() {
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
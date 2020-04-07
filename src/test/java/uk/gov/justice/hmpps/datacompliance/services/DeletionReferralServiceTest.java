package uk.gov.justice.hmpps.datacompliance.services;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.Booking;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.OffenderWithBookings;
import uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted.OffenderDeletionGrantedEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DeletionReferralServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final String OFFENDER_NUMBER = "A1234AA";

    @Mock
    private OffenderDeletionReferralRepository repository;

    @Mock
    private OffenderDeletionGrantedEventPusher eventPusher;

    private DeletionReferralService service;

    @BeforeEach
    void setUp() {
        service = new DeletionReferralService(TimeSource.of(NOW), repository, eventPusher);
    }

    @Test
    void handlePendingDeletionReferral() {

        service.handlePendingDeletion(OffenderPendingDeletionEvent.builder()
                .offenderIdDisplay(OFFENDER_NUMBER)
                .firstName("John")
                .middleName("Middle")
                .lastName("Smith")
                .birthDate(LocalDate.of(1969, 1, 1))
                .offender(OffenderWithBookings.builder()
                        .offenderId(1L)
                        .booking(new Booking(2L))
                        .build())
                .build());

        verifyReferralPersisted();
        verify(eventPusher).sendEvent(OFFENDER_NUMBER);
    }

    private void verifyReferralPersisted() {

        final var referralCaptor = ArgumentCaptor.forClass(OffenderDeletionReferral.class);

        verify(repository).save(referralCaptor.capture());

        final var persistedReferral = referralCaptor.getValue();

        assertThat(persistedReferral.getOffenderNo()).isEqualTo(OFFENDER_NUMBER);
        assertThat(persistedReferral.getFirstName()).isEqualTo("John");
        assertThat(persistedReferral.getMiddleName()).isEqualTo("Middle");
        assertThat(persistedReferral.getLastName()).isEqualTo("Smith");
        assertThat(persistedReferral.getBirthDate()).isEqualTo(LocalDate.of(1969, 1, 1));

        assertThat(persistedReferral.getReferredOffenderBookings()).hasSize(1);
        final var offenderBooking = persistedReferral.getReferredOffenderBookings().get(0);

        assertThat(offenderBooking.getOffenderId()).isEqualTo(1L);
        assertThat(offenderBooking.getOffenderBookId()).isEqualTo(2L);
    }
}
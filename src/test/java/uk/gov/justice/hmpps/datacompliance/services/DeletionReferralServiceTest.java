package uk.gov.justice.hmpps.datacompliance.services;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.events.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.dto.OffenderPendingDeletionEvent.Booking;
import uk.gov.justice.hmpps.datacompliance.events.dto.OffenderPendingDeletionEvent.OffenderWithBookings;
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
    
    @Mock
    private OffenderDeletionReferralRepository repository;
    
    private DeletionReferralService service;

    @BeforeEach
    void setUp() {
        service = new DeletionReferralService(TimeSource.of(NOW), repository);
    }

    @Test
    void storeOffenderPendingDeletionReferral() {

        final var referralCaptor = ArgumentCaptor.forClass(OffenderDeletionReferral.class);

        service.storeOffenderDeletionReferral(OffenderPendingDeletionEvent.builder()
                .offenderIdDisplay("A1234AA")
                .firstName("John")
                .middleName("Middle")
                .lastName("Smith")
                .birthDate(LocalDate.of(1969, 1, 1))
                .offender(OffenderWithBookings.builder()
                        .offenderId(1L)
                        .booking(new Booking(2L))
                        .build())
                .build());

        verify(repository).save(referralCaptor.capture());

        final var persistedReferral = referralCaptor.getValue();

        assertThat(persistedReferral.getOffenderNo()).isEqualTo("A1234AA");
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
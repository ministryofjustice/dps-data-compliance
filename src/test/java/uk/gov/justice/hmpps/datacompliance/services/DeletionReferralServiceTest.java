package uk.gov.justice.hmpps.datacompliance.services;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.Booking;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.OffenderWithBookings;
import uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted.OffenderDeletionGrantedEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderBooking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionType.DELETED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionType.DELETION_GRANTED;

@ExtendWith(MockitoExtension.class)
class DeletionReferralServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final String OFFENDER_NUMBER = "A1234AA";

    @Mock
    private OffenderDeletionReferralRepository repository;

    @Mock
    private OffenderDeletionGrantedEventPusher eventPusher;

    @Mock
    private RetentionService retentionService;

    private DeletionReferralService referralService;

    @BeforeEach
    void setUp() {
        referralService = new DeletionReferralService(TimeSource.of(NOW), repository, eventPusher, retentionService);
    }

    @Test
    void handlePendingDeletionWhenOffenderEligibleForDeletion() {

        when(retentionService.isOffenderEligibleForDeletion(new OffenderNumber(OFFENDER_NUMBER)))
                .thenReturn(true);

        referralService.handlePendingDeletion(generatePendingDeletionEvent());

        verifyReferralPersisted();
        verify(eventPusher).grantDeletion(OFFENDER_NUMBER);
    }

    @Test
    void handlePendingDeletionWhenOffenderShouldBeRetained() {

        when(retentionService.isOffenderEligibleForDeletion(new OffenderNumber(OFFENDER_NUMBER)))
                .thenReturn(false);

        referralService.handlePendingDeletion(generatePendingDeletionEvent());

        verify(repository, never()).save(any());
        verify(eventPusher, never()).grantDeletion(any());
    }

    @Test
    void handleDeletionComplete() {

        final var existingReferral = spy(OffenderDeletionReferral.builder().offenderNo(OFFENDER_NUMBER).build());
        final var referralResolution = spy(ReferralResolution.builder().resolutionType(DELETION_GRANTED).build());

        when(existingReferral.getReferralResolution()).thenReturn(Optional.of(referralResolution));
        when(repository.findById(123L)).thenReturn(Optional.of(existingReferral));

        referralService.handleDeletionComplete(OffenderDeletionCompleteEvent.builder()
                .offenderIdDisplay(OFFENDER_NUMBER)
                .referralId(123L)
                .build());

        InOrder inOrder = inOrder(referralResolution, repository);
        inOrder.verify(referralResolution).setResolutionDateTime(NOW);
        inOrder.verify(referralResolution).setResolutionType(DELETED);
        inOrder.verify(repository).save(existingReferral);
    }

    @Test
    void handleDeletionCompleteThrowsIfReferralNotFound() {

        when(repository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                referralService.handleDeletionComplete(OffenderDeletionCompleteEvent.builder().referralId(123L).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot retrieve referral record for id: '123'");

        verify(repository, never()).save(any());
    }

    @Test
    void handleDeletionCompleteThrowsIfOffenderNumbersDoNotMatch() {

        when(repository.findById(123L)).thenReturn(Optional.of(OffenderDeletionReferral.builder()
                .referralId(123L)
                .offenderNo("offender1")
                .build()));

        assertThatThrownBy(() ->
                referralService.handleDeletionComplete(OffenderDeletionCompleteEvent.builder()
                        .offenderIdDisplay("offender2")
                        .referralId(123L)
                        .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Offender number 'offender1' of referral '123' does not match 'offender2'");

        verify(repository, never()).save(any());
    }

    @Test
    void handleDeletionCompleteThrowsIfResolutionNotFound() {

        when(repository.findById(123L)).thenReturn(Optional.of(OffenderDeletionReferral.builder()
                .referralId(123L)
                .offenderNo(OFFENDER_NUMBER)
                .build()));

        assertThatThrownBy(() ->
                referralService.handleDeletionComplete(OffenderDeletionCompleteEvent.builder()
                        .offenderIdDisplay(OFFENDER_NUMBER)
                        .referralId(123L)
                        .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Referral '123' does not have expected resolution type");

        verify(repository, never()).save(any());
    }

    @Test
    void handleDeletionCompleteThrowsIfResolutionTypeUnexpected() {

        final var existingReferral = OffenderDeletionReferral.builder()
                .offenderNo(OFFENDER_NUMBER)
                .referralId(123L)
                .build();
        existingReferral.setReferralResolution(ReferralResolution.builder().resolutionType(DELETED).build());

        when(repository.findById(123L)).thenReturn(Optional.of(existingReferral));

        assertThatThrownBy(() ->
                referralService.handleDeletionComplete(OffenderDeletionCompleteEvent.builder()
                        .offenderIdDisplay(OFFENDER_NUMBER)
                        .referralId(123L)
                        .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Referral '123' does not have expected resolution type");
    }

    private OffenderPendingDeletionEvent generatePendingDeletionEvent() {
        return OffenderPendingDeletionEvent.builder()
                .offenderIdDisplay(OFFENDER_NUMBER)
                .firstName("John")
                .middleName("Middle")
                .lastName("Smith")
                .birthDate(LocalDate.of(1969, 1, 1))
                .offender(OffenderWithBookings.builder()
                        .offenderId(1L)
                        .booking(new Booking(2L))
                        .build())
                .build();
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
        assertThat(persistedReferral.getReceivedDateTime()).isEqualTo(NOW);

        assertThat(persistedReferral.getReferredOffenderBookings()).hasSize(1);
        verifyPersistedBooking(persistedReferral.getReferredOffenderBookings().get(0));
        verifyPersistedResolution(persistedReferral.getReferralResolution().orElseThrow());
    }

    private void verifyPersistedBooking(final ReferredOffenderBooking offenderBooking) {
        assertThat(offenderBooking.getOffenderId()).isEqualTo(1L);
        assertThat(offenderBooking.getOffenderBookId()).isEqualTo(2L);
    }

    private void verifyPersistedResolution(final ReferralResolution resolution) {
        assertThat(resolution.getResolutionDateTime()).isEqualTo(NOW);
        assertThat(resolution.getResolutionType()).isEqualTo(DELETION_GRANTED);
    }
}
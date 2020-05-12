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
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.OffenderBooking;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.OffenderWithBookings;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.completed.OffenderDeletionCompleteEventPusher;
import uk.gov.justice.hmpps.datacompliance.events.publishers.deletion.granted.OffenderDeletionGrantedEventPusher;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent.Booking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderBooking;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.services.referral.DeletionReferralService;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent.OffenderWithBookings.builder;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.RETAINED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

@ExtendWith(MockitoExtension.class)
class DeletionReferralServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final long BATCH_ID = 123L;
    private static final String OFFENDER_NUMBER = "A1234AA";

    @Mock
    private OffenderDeletionBatchRepository batchRepository;

    @Mock
    private OffenderDeletionReferralRepository referralRepository;

    @Mock
    private OffenderDeletionGrantedEventPusher deletionGrantedEventPusher;

    @Mock
    private OffenderDeletionCompleteEventPusher deletionCompleteEventPusher;

    @Mock
    private RetentionService retentionService;

    @Mock
    private OffenderDeletionBatch batch;

    private DeletionReferralService referralService;

    @BeforeEach
    void setUp() {
        referralService = new DeletionReferralService(
                TimeSource.of(NOW),
                batchRepository,
                referralRepository,
                deletionGrantedEventPusher,
                deletionCompleteEventPusher,
                retentionService);
    }

    @Test
    void handlePendingDeletionWhenOffenderEligibleForDeletion() {

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(retentionService.conductRetentionChecks(new OffenderNumber(OFFENDER_NUMBER)))
                .thenReturn(List.of(new RetentionCheckManual(RETENTION_NOT_REQUIRED)));

        referralService.handlePendingDeletion(generatePendingDeletionEvent());

        final var referral = verifyReferralPersisted();
        assertThat(referral.getOffenderBookings()).hasSize(1);
        verifyPersistedBooking(referral.getOffenderBookings().get(0));
        verifyPersistedResolution(referral.getReferralResolution().orElseThrow(), DELETION_GRANTED);
        verify(deletionGrantedEventPusher).grantDeletion(eq(new OffenderNumber(OFFENDER_NUMBER)), any());
    }

    @Test
    void handlePendingDeletionWhenOffenderShouldBeRetained() {

        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(retentionService.conductRetentionChecks(new OffenderNumber(OFFENDER_NUMBER)))
                .thenReturn(List.of(new RetentionCheckManual(RETENTION_REQUIRED)));

        referralService.handlePendingDeletion(generatePendingDeletionEvent());

        final var referral = verifyReferralPersisted();
        verifyPersistedResolution(referral.getReferralResolution().orElseThrow(), RETAINED);
        verify(deletionGrantedEventPusher, never()).grantDeletion(any(), anyLong());
    }

    @Test
    void handleDeletionComplete() {

        final var referralResolution = spy(ReferralResolution.builder().resolutionStatus(DELETION_GRANTED).build());
        final var existingReferral = generateOffenderDeletionReferral();
        existingReferral.setReferralResolution(referralResolution);

        when(referralRepository.findById(123L)).thenReturn(Optional.of(existingReferral));
        when(referralRepository.save(existingReferral)).thenReturn(existingReferral);

        referralService.handleDeletionComplete(OffenderDeletionCompleteEvent.builder()
                .offenderIdDisplay(OFFENDER_NUMBER)
                .referralId(123L)
                .build());

        final var referral = verifyReferralPersisted();
        verifyPersistedResolution(referral.getReferralResolution().orElseThrow(), DELETED);
        verify(deletionCompleteEventPusher).sendEvent(
                uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionCompleteEvent.builder()
                        .offenderIdDisplay(OFFENDER_NUMBER)
                        .offender(builder()
                                .offenderId(1L)
                                .booking(new Booking(11L))
                                .booking(new Booking(12L))
                                .build())
                        .offender(builder()
                                .offenderId(2L)
                                .booking(new Booking(21L))
                                .build())
                        .build());
    }

    @Test
    void handleDeletionCompleteThrowsIfReferralNotFound() {

        when(referralRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                referralService.handleDeletionComplete(OffenderDeletionCompleteEvent.builder().referralId(123L).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot retrieve referral record for id: '123'");

        verify(referralRepository, never()).save(any());
    }

    @Test
    void handleDeletionCompleteThrowsIfOffenderNumbersDoNotMatch() {

        when(referralRepository.findById(123L)).thenReturn(Optional.of(OffenderDeletionReferral.builder()
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

        verify(referralRepository, never()).save(any());
    }

    @Test
    void handleDeletionCompleteThrowsIfResolutionNotFound() {

        when(referralRepository.findById(123L)).thenReturn(Optional.of(OffenderDeletionReferral.builder()
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

        verify(referralRepository, never()).save(any());
    }

    @Test
    void handleDeletionCompleteThrowsIfResolutionTypeUnexpected() {

        final var existingReferral = OffenderDeletionReferral.builder()
                .offenderNo(OFFENDER_NUMBER)
                .referralId(123L)
                .build();
        existingReferral.setReferralResolution(ReferralResolution.builder().resolutionStatus(DELETED).build());

        when(referralRepository.findById(123L)).thenReturn(Optional.of(existingReferral));

        assertThatThrownBy(() ->
                referralService.handleDeletionComplete(OffenderDeletionCompleteEvent.builder()
                        .offenderIdDisplay(OFFENDER_NUMBER)
                        .referralId(123L)
                        .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Referral '123' does not have expected resolution type");
    }

    @Test
    void handleReferralComplete() {

        when(batchRepository.findById(123L)).thenReturn(Optional.of(batch));

        referralService.handleReferralComplete(new OffenderPendingDeletionReferralCompleteEvent(123L));

        InOrder inOrder = inOrder(batch, batchRepository);
        inOrder.verify(batch).setReferralCompletionDateTime(NOW);
        inOrder.verify(batchRepository).save(batch);
    }

    @Test
    void handleReferralCompleteThrowsIfBatchNotFound() {

        when(batchRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                referralService.handleReferralComplete(new OffenderPendingDeletionReferralCompleteEvent(123L)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot find batch with id: '123'");

        verify(batchRepository, never()).save(any());
    }

    private OffenderPendingDeletionEvent generatePendingDeletionEvent() {
        return OffenderPendingDeletionEvent.builder()
                .batchId(BATCH_ID)
                .offenderIdDisplay(OFFENDER_NUMBER)
                .firstName("John")
                .middleName("Middle")
                .lastName("Smith")
                .birthDate(LocalDate.of(1969, 1, 1))
                .offender(OffenderWithBookings.builder()
                        .offenderId(1L)
                        .offenderBooking(new OffenderBooking(2L))
                        .build())
                .build();
    }

    private OffenderDeletionReferral verifyReferralPersisted() {

        final var referralCaptor = ArgumentCaptor.forClass(OffenderDeletionReferral.class);

        verify(referralRepository).save(referralCaptor.capture());

        final var persistedReferral = referralCaptor.getValue();

        assertThat(persistedReferral.getOffenderDeletionBatch()).isEqualTo(batch);
        assertThat(persistedReferral.getOffenderNo()).isEqualTo(OFFENDER_NUMBER);
        assertThat(persistedReferral.getFirstName()).isEqualTo("John");
        assertThat(persistedReferral.getMiddleName()).isEqualTo("Middle");
        assertThat(persistedReferral.getLastName()).isEqualTo("Smith");
        assertThat(persistedReferral.getBirthDate()).isEqualTo(LocalDate.of(1969, 1, 1));
        assertThat(persistedReferral.getReceivedDateTime()).isEqualTo(NOW);

        return persistedReferral;
    }

    private void verifyPersistedBooking(final ReferredOffenderBooking offenderBooking) {
        assertThat(offenderBooking.getOffenderId()).isEqualTo(1L);
        assertThat(offenderBooking.getOffenderBookId()).isEqualTo(2L);
    }

    private void verifyPersistedResolution(final ReferralResolution resolution, final ResolutionStatus type) {
        assertThat(resolution.getResolutionDateTime()).isEqualTo(NOW);
        assertThat(resolution.getResolutionStatus()).isEqualTo(type);
    }

    private OffenderDeletionReferral generateOffenderDeletionReferral() {

        final var referral = spy(OffenderDeletionReferral.builder()
                .offenderDeletionBatch(batch)
                .offenderNo(OFFENDER_NUMBER)
                .firstName("John")
                .middleName("Middle")
                .lastName("Smith")
                .birthDate(LocalDate.of(1969, 1, 1))
                .receivedDateTime(NOW)
                .build());

        referral.addReferredOffenderBooking(ReferredOffenderBooking.builder().offenderId(1L).offenderBookId(11L).build());
        referral.addReferredOffenderBooking(ReferredOffenderBooking.builder().offenderId(1L).offenderBookId(12L).build());
        referral.addReferredOffenderBooking(ReferredOffenderBooking.builder().offenderId(2L).offenderBookId(21L).build());

        return referral;
    }
}
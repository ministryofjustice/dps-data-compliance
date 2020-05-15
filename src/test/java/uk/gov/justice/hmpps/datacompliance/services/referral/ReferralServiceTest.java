package uk.gov.justice.hmpps.datacompliance.services.referral;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.OffenderBooking;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionEvent.OffenderWithBookings;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralCompleteEvent;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.services.retention.ActionableRetentionCheck;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final long BATCH_ID = 123L;
    private static final String OFFENDER_NUMBER = "A1234AA";

    @Mock
    private OffenderDeletionBatchRepository batchRepository;

    @Mock
    private RetentionService retentionService;

    @Mock
    private OffenderDeletionBatch batch;

    @Mock
    private ReferralResolutionService referralResolutionService;

    private ReferralService referralService;

    @BeforeEach
    void setUp() {
        referralService = new ReferralService(
                TimeSource.of(NOW),
                batchRepository,
                retentionService,
                referralResolutionService);
    }

    @Test
    void handlePendingDeletionReferral() {

        final var retentionCheck = mock(ActionableRetentionCheck.class);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(retentionService.conductRetentionChecks(new OffenderNumber(OFFENDER_NUMBER)))
                .thenReturn(List.of(retentionCheck));

        referralService.handlePendingDeletionReferral(generatePendingDeletionEvent());

        final var referral = ArgumentCaptor.forClass(OffenderDeletionReferral.class);
        verify(referralResolutionService).processReferral(referral.capture(), eq(List.of(retentionCheck)));
        verifyReferral(referral.getValue());
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

    private void verifyReferral(final OffenderDeletionReferral referral) {

        assertThat(referral.getOffenderDeletionBatch()).isEqualTo(batch);
        assertThat(referral.getOffenderNo()).isEqualTo(OFFENDER_NUMBER);
        assertThat(referral.getFirstName()).isEqualTo("John");
        assertThat(referral.getMiddleName()).isEqualTo("Middle");
        assertThat(referral.getLastName()).isEqualTo("Smith");
        assertThat(referral.getBirthDate()).isEqualTo(LocalDate.of(1969, 1, 1));
        assertThat(referral.getReceivedDateTime()).isEqualTo(NOW);

        verifyBooking(referral);
    }

    private void verifyBooking(final OffenderDeletionReferral referral) {

        assertThat(referral.getOffenderBookings()).hasSize(1);

        final var offenderBooking = referral.getOffenderBookings().get(0);
        assertThat(offenderBooking.getOffenderId()).isEqualTo(1L);
        assertThat(offenderBooking.getOffenderBookId()).isEqualTo(2L);
    }
}

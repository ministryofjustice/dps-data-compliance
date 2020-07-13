package uk.gov.justice.hmpps.datacompliance.services.deletion;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderDeletionGrant;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderDeletionComplete;
import uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionComplete.Booking;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sns.OffenderDeletionCompleteEventPusher;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferredOffenderAlias;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionComplete.OffenderWithBookings.builder;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;

@ExtendWith(MockitoExtension.class)
class DeletionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final String OFFENDER_NUMBER = "A1234AA";
    private static final long REFERRAL_ID = 123;
    private static final long OFFENDER_ID = 456;
    private static final long OFFENDER_BOOK_ID = 788;

    @Mock
    private OffenderDeletionReferralRepository referralRepository;

    @Mock
    private DataComplianceEventPusher deletionGrantedEventPusher;

    @Mock
    private OffenderDeletionCompleteEventPusher deletionCompleteEventPusher;

    @Mock
    private OffenderDeletionBatch batch;

    private DeletionService deletionService;

    @BeforeEach
    void setUp() {
        deletionService = new DeletionService(
                TimeSource.of(NOW),
                referralRepository,
                deletionCompleteEventPusher,
                deletionGrantedEventPusher,
                DataComplianceProperties.builder().deletionGrantEnabled(true).build());
    }

    @Test
    void grantDeletion() {

        final var referral = OffenderDeletionReferral.builder()
                .referralId(REFERRAL_ID)
                .offenderNo(OFFENDER_NUMBER)
                .build();

        referral.addReferredOffenderAlias(ReferredOffenderAlias.builder()
                .offenderId(OFFENDER_ID)
                .offenderBookId(OFFENDER_BOOK_ID)
                .build());

        deletionService.grantDeletion(referral);

        verify(deletionGrantedEventPusher).grantDeletion(
                OffenderDeletionGrant.builder()
                        .offenderNumber(new OffenderNumber(OFFENDER_NUMBER))
                        .referralId(REFERRAL_ID)
                        .offenderId(OFFENDER_ID)
                        .offenderBookId(OFFENDER_BOOK_ID)
                        .build());
    }

    @Test
    void grantDeletionWithNoBooking() {

        final var referral = OffenderDeletionReferral.builder()
                .referralId(REFERRAL_ID)
                .offenderNo(OFFENDER_NUMBER)
                .build();

        referral.addReferredOffenderAlias(ReferredOffenderAlias.builder().offenderId(OFFENDER_ID).build());

        deletionService.grantDeletion(referral);

        verify(deletionGrantedEventPusher).grantDeletion(
                OffenderDeletionGrant.builder()
                        .offenderNumber(new OffenderNumber(OFFENDER_NUMBER))
                        .referralId(REFERRAL_ID)
                        .offenderId(OFFENDER_ID)
                        .build());
    }

    @Test
    void grantDeletionDisabled() {
        deletionService = new DeletionService(
                TimeSource.of(NOW),
                referralRepository,
                deletionCompleteEventPusher,
                deletionGrantedEventPusher,
                DataComplianceProperties.builder().deletionGrantEnabled(false).build());

        deletionService.grantDeletion(OffenderDeletionReferral.builder()
                .referralId(REFERRAL_ID)
                .offenderNo(OFFENDER_NUMBER)
                .build());

        verifyNoInteractions(deletionGrantedEventPusher);
    }

    @Test
    void handleDeletionComplete() {

        final var existingReferral = generateOffenderDeletionReferral();

        when(referralRepository.findById(REFERRAL_ID)).thenReturn(Optional.of(existingReferral));
        when(referralRepository.save(existingReferral)).thenReturn(existingReferral);

        deletionService.handleDeletionComplete(OffenderDeletionComplete.builder()
                .offenderIdDisplay(OFFENDER_NUMBER)
                .referralId(REFERRAL_ID)
                .build());

        final var resolution = existingReferral.getReferralResolution().orElseThrow();
        assertThat(resolution.getResolutionStatus()).isEqualTo(DELETED);
        assertThat(resolution.getResolutionDateTime()).isEqualTo(NOW);

        verify(deletionCompleteEventPusher).sendEvent(
                uk.gov.justice.hmpps.datacompliance.events.publishers.dto.OffenderDeletionComplete.builder()
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

        when(referralRepository.findById(REFERRAL_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                deletionService.handleDeletionComplete(OffenderDeletionComplete.builder().referralId(123L).build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Cannot retrieve referral record for id: '123'");

        verify(referralRepository, never()).save(any());
    }

    @Test
    void handleDeletionCompleteThrowsIfOffenderNumbersDoNotMatch() {

        when(referralRepository.findById(REFERRAL_ID)).thenReturn(Optional.of(OffenderDeletionReferral.builder()
                .referralId(REFERRAL_ID)
                .offenderNo("offender1")
                .build()));

        assertThatThrownBy(() ->
                deletionService.handleDeletionComplete(OffenderDeletionComplete.builder()
                        .offenderIdDisplay("offender2")
                        .referralId(REFERRAL_ID)
                        .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Offender number 'offender1' of referral '123' does not match 'offender2'");

        verify(referralRepository, never()).save(any());
    }

    @Test
    void handleDeletionCompleteThrowsIfResolutionNotFound() {

        when(referralRepository.findById(REFERRAL_ID)).thenReturn(Optional.of(OffenderDeletionReferral.builder()
                .referralId(REFERRAL_ID)
                .offenderNo(OFFENDER_NUMBER)
                .build()));

        assertThatThrownBy(() ->
                deletionService.handleDeletionComplete(OffenderDeletionComplete.builder()
                        .offenderIdDisplay(OFFENDER_NUMBER)
                        .referralId(REFERRAL_ID)
                        .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Referral '123' does not have expected resolution type");

        verify(referralRepository, never()).save(any());
    }

    @Test
    void handleDeletionCompleteThrowsIfResolutionTypeUnexpected() {

        final var existingReferral = OffenderDeletionReferral.builder()
                .offenderNo(OFFENDER_NUMBER)
                .referralId(REFERRAL_ID)
                .build();
        existingReferral.setReferralResolution(ReferralResolution.builder().resolutionStatus(DELETED).build());

        when(referralRepository.findById(REFERRAL_ID)).thenReturn(Optional.of(existingReferral));

        assertThatThrownBy(() ->
                deletionService.handleDeletionComplete(OffenderDeletionComplete.builder()
                        .offenderIdDisplay(OFFENDER_NUMBER)
                        .referralId(REFERRAL_ID)
                        .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Referral '123' does not have expected resolution type");
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

        referral.addReferredOffenderAlias(ReferredOffenderAlias.builder().offenderId(1L).offenderBookId(11L).build());
        referral.addReferredOffenderAlias(ReferredOffenderAlias.builder().offenderId(1L).offenderBookId(12L).build());
        referral.addReferredOffenderAlias(ReferredOffenderAlias.builder().offenderId(2L).offenderBookId(21L).build());

        referral.setReferralResolution(ReferralResolution.builder()
                .resolutionStatus(DELETION_GRANTED)
                .build()
                .addRetentionCheck(new RetentionCheckManual(RETENTION_NOT_REQUIRED)));

        return referral;
    }
}

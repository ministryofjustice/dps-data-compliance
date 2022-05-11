package uk.gov.justice.hmpps.datacompliance.services.referral;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderToCheck;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.AdHocOffenderDeletion;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion.OffenderAlias;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletion.OffenderBooking;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.OffenderPendingDeletionReferralComplete;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.ProvisionalDeletionReferralResult;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.services.retention.ActionableRetentionCheck;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.AD_HOC;

@ExtendWith(MockitoExtension.class)
class ReferralServiceTest {

    public static final String FIRST_NAME = "John";
    public static final String MIDDLE_NAME = "Middle";
    public static final String LAST_NAME = "Smith";
    public static final LocalDate DOB = LocalDate.of(1969, 1, 1);
    public static final String PNC = "14/663516A";
    public static final String CRO = "569151/08";
    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);
    private static final long BATCH_ID = 123L;
    private static final String OFFENDER_NUMBER = "A1234AA";
    private static final long REFERRAL_ID = 123L;
    private static final String AGENCY_LOCATION_ID = "LEI";
    private static final Set<String> OFFENCE_CODES = Set.of("offenceCode");
    private static final Set<String> ALERT_CODES = Set.of("alertCode");
    @Mock
    private OffenderDeletionBatchRepository batchRepository;

    @Mock
    private RetentionService retentionService;

    @Mock
    private OffenderDeletionBatch batch;

    @Mock
    private ReferralResolutionService referralResolutionService;

    @Mock
    private DataComplianceEventPusher eventPusher;

    @Mock
    private OffenderDeletionReferralRepository offenderDeletionReferralRepository;

    private ReferralService referralService;

    @BeforeEach
    void setUp() {
        referralService = new ReferralService(
            TimeSource.of(NOW),
            batchRepository,
            retentionService,
            referralResolutionService,
            eventPusher,
            offenderDeletionReferralRepository);
    }

    @Test
    void handleAdHocDeletion() {

        final var batch = ArgumentCaptor.forClass(OffenderDeletionBatch.class);

        when(batchRepository.save(batch.capture()))
            .thenReturn(OffenderDeletionBatch.builder().batchId(BATCH_ID).build());

        referralService.handleAdHocDeletion(new AdHocOffenderDeletion(OFFENDER_NUMBER, "Some reason"));

        assertThat(batch.getValue().getRequestDateTime()).isEqualTo(NOW);
        assertThat(batch.getValue().getCommentText()).isEqualTo("Some reason");
        assertThat(batch.getValue().getBatchType()).isEqualTo(AD_HOC);

        verify(eventPusher).requestAdHocReferral(new OffenderNumber(OFFENDER_NUMBER), BATCH_ID);
    }

    @Test
    void handlePendingDeletionReferral() {

        final var retentionCheck = mock(ActionableRetentionCheck.class);
        when(batchRepository.findById(BATCH_ID)).thenReturn(Optional.of(batch));
        when(retentionService.conductRetentionChecks(OffenderToCheck.builder()
            .offenderNumber(new OffenderNumber(OFFENDER_NUMBER))
            .firstName(FIRST_NAME)
            .middleName(MIDDLE_NAME)
            .lastName(LAST_NAME)
            .pnc(PNC)
            .cro(CRO)
            .bookingNos(Set.of("B07236", "V30240")).build()))
            .thenReturn(List.of(retentionCheck));

        referralService.handlePendingDeletionReferral(generatePendingDeletionEvent());

        final var referral = ArgumentCaptor.forClass(OffenderDeletionReferral.class);
        verify(referralResolutionService).processReferral(referral.capture(), eq(List.of(retentionCheck)));
        verifyReferral(referral.getValue());
    }

    @Test
    void handleReferralComplete() {

        when(batchRepository.findById(123L)).thenReturn(Optional.of(batch));

        referralService.handleReferralComplete(new OffenderPendingDeletionReferralComplete(123L, 4L, 5L));

        InOrder inOrder = inOrder(batch, batchRepository);
        inOrder.verify(batch).setReferralCompletionDateTime(NOW);
        inOrder.verify(batch).setRemainingInWindow(1);
        inOrder.verify(batchRepository).save(batch);
    }

    @Test
    void handleReferralCompleteThrowsIfBatchNotFound() {

        when(batchRepository.findById(123L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            referralService.handleReferralComplete(new OffenderPendingDeletionReferralComplete(123L, 4L, 5L)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot find batch with id: '123'");

        verify(batchRepository, never()).save(any());
    }

    @Test
    void handleProvisionalDeletionReferralResult() {

        final var referral = buildOffenderDeletionReferral();
        final var retentionCheck = mock(ActionableRetentionCheck.class);

        when(retentionService.conductRetentionChecks(offenderToCheck()))
            .thenReturn(List.of(retentionCheck));

        when(offenderDeletionReferralRepository.findById(REFERRAL_ID))
            .thenReturn(Optional.of(referral));

        referralService.handleProvisionalDeletionReferralResult(new ProvisionalDeletionReferralResult(referral.getReferralId(), OFFENDER_NUMBER,
            false, AGENCY_LOCATION_ID, OFFENCE_CODES, ALERT_CODES));

        verify(referralResolutionService).processProvisionalDeletionReferral(referral, List.of(retentionCheck));
    }

    @Test
    void handleProvisionalDeletionReferralResultWhenNoPreviousAgencyLocationIdentified() {

        final var referral = OffenderDeletionReferral.builder()
            .referralId(REFERRAL_ID)
            .offenderNo(OFFENDER_NUMBER)
            .build();

        final var retentionCheck = mock(ActionableRetentionCheck.class);

        when(retentionService.conductRetentionChecks(offenderToCheck()))
            .thenReturn(List.of(retentionCheck));

        when(offenderDeletionReferralRepository.findById(REFERRAL_ID))
            .thenReturn(Optional.of(referral));

        referralService.handleProvisionalDeletionReferralResult(new ProvisionalDeletionReferralResult(referral.getReferralId(), OFFENDER_NUMBER,
            false, null, OFFENCE_CODES, ALERT_CODES));

        verify(referralResolutionService).processProvisionalDeletionReferral(referral, List.of(retentionCheck));
    }

    @Test
    void handleProvisionalDeletionReferralResultRetainsIfLastKnownLocationDoesNotMatch() {

        final var referral = OffenderDeletionReferral.builder()
            .referralId(REFERRAL_ID)
            .offenderNo(OFFENDER_NUMBER)
            .agencyLocationId("some_different_agency_loc")
            .build();

        when(offenderDeletionReferralRepository.findById(REFERRAL_ID))
            .thenReturn(Optional.of(referral));


        referralService.handleProvisionalDeletionReferralResult(new ProvisionalDeletionReferralResult(referral.getReferralId(), OFFENDER_NUMBER,
            false, AGENCY_LOCATION_ID, OFFENCE_CODES, ALERT_CODES));

        verify(referralResolutionService).updateReferralChangesIdentified(referral);
    }


    @Test
    void handleProvisionalDeletionReferralResultRetainsIfSubsequentChangeIdentified() {

        final var referral = OffenderDeletionReferral.builder()
            .referralId(REFERRAL_ID)
            .offenderNo(OFFENDER_NUMBER)
            .agencyLocationId(AGENCY_LOCATION_ID)
            .build();

        when(offenderDeletionReferralRepository.findById(REFERRAL_ID))
            .thenReturn(Optional.of(referral));

        referralService.handleProvisionalDeletionReferralResult(new ProvisionalDeletionReferralResult(referral.getReferralId(), OFFENDER_NUMBER,
            true, AGENCY_LOCATION_ID, OFFENCE_CODES, ALERT_CODES));


        verify(referralResolutionService).updateReferralChangesIdentified(referral);
    }

    @Test
    void handleProvisionalDeletionReferralResultThrowsIfOffenderNumberIsNull() {

        assertThatThrownBy(() ->
            referralService.handleProvisionalDeletionReferralResult(new ProvisionalDeletionReferralResult(REFERRAL_ID, null,
                false, AGENCY_LOCATION_ID, OFFENCE_CODES, ALERT_CODES)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Null offender number");
    }

    @Test
    void handleProvisionalDeletionReferralResultThrowsIfReferralIdIsNull() {

        assertThatThrownBy(() ->
            referralService.handleProvisionalDeletionReferralResult(new ProvisionalDeletionReferralResult(null, OFFENDER_NUMBER,
                false, AGENCY_LOCATION_ID, OFFENCE_CODES, ALERT_CODES)))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("Invalid referral id received: 'null'");
    }


    private OffenderToCheck offenderToCheck() {
        return OffenderToCheck.builder()
            .offenderNumber(new OffenderNumber(OFFENDER_NUMBER))
            .offenceCodes(OFFENCE_CODES)
            .alertCodes(ALERT_CODES)
            .build();
    }

    private OffenderPendingDeletion generatePendingDeletionEvent() {
        return OffenderPendingDeletion.builder()
            .batchId(BATCH_ID)
            .offenderIdDisplay(OFFENDER_NUMBER)
            .firstName(FIRST_NAME)
            .middleName(MIDDLE_NAME)
            .lastName(LAST_NAME)
            .birthDate(DOB)
            .pnc(PNC)
            .cro(CRO)
            .offenderAlias(OffenderAlias.builder()
                .offenderId(123L)
                .offenderBooking(OffenderBooking.builder().offenderBookId(456L).bookingNo("B07236").build())
                .offenderBooking(OffenderBooking.builder().offenderBookId(789L).bookingNo("V30240").build())
                .build())
            .offenderAlias(OffenderAlias.builder().offenderId(321L).build())
            .build();
    }

    private void verifyReferral(final OffenderDeletionReferral referral) {

        assertThat(referral.getOffenderDeletionBatch()).isEqualTo(batch);
        assertThat(referral.getOffenderNo()).isEqualTo(OFFENDER_NUMBER);
        assertThat(referral.getFirstName()).isEqualTo(FIRST_NAME);
        assertThat(referral.getMiddleName()).isEqualTo(MIDDLE_NAME);
        assertThat(referral.getLastName()).isEqualTo(LAST_NAME);
        assertThat(referral.getBirthDate()).isEqualTo(DOB);
        assertThat(referral.getReceivedDateTime()).isEqualTo(NOW);

        verifyBooking(referral);
    }

    private void verifyBooking(final OffenderDeletionReferral referral) {

        assertThat(referral.getOffenderAliases()).hasSize(3);

        assertThat(referral.getOffenderAliases().get(0).getOffenderId()).isEqualTo(123L);
        assertThat(referral.getOffenderAliases().get(0).getOffenderBookId()).isEqualTo(456L);

        assertThat(referral.getOffenderAliases().get(1).getOffenderId()).isEqualTo(123L);
        assertThat(referral.getOffenderAliases().get(1).getOffenderBookId()).isEqualTo(789L);

        assertThat(referral.getOffenderAliases().get(2).getOffenderId()).isEqualTo(321L);
        assertThat(referral.getOffenderAliases().get(2).getOffenderBookId()).isNull();
    }

    private OffenderDeletionReferral buildOffenderDeletionReferral() {
        return OffenderDeletionReferral.builder()
            .referralId(REFERRAL_ID)
            .offenderNo(OFFENDER_NUMBER)
            .agencyLocationId(AGENCY_LOCATION_ID)
            .build();
    }
}

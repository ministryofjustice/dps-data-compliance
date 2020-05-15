package uk.gov.justice.hmpps.datacompliance.services.referral;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionReferral;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionReferralRepository;
import uk.gov.justice.hmpps.datacompliance.services.deletion.DeletionService;
import uk.gov.justice.hmpps.datacompliance.services.retention.ActionableRetentionCheck;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.DELETION_GRANTED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.RETAINED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

@ExtendWith(MockitoExtension.class)
class ReferralResolutionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Mock
    private OffenderDeletionReferralRepository referralRepository;

    @Mock
    private DeletionService deletionService;

    private ReferralResolutionService referralResolutionService;

    @BeforeEach
    void setUp() {
        referralResolutionService = new ReferralResolutionService(
                TimeSource.of(NOW),
                deletionService,
                referralRepository);
    }

    @Test
    void processReferralWhenOffenderEligibleForDeletion() {

        final var referral = mock(OffenderDeletionReferral.class);
        final var retentionCheck = new RetentionCheckManual(RETENTION_NOT_REQUIRED);

        referralResolutionService.processReferral(referral,
                List.of(new ActionableRetentionCheck(retentionCheck)));

        verifyPersistence(referral, DELETION_GRANTED, retentionCheck);
        verify(deletionService).grantDeletion(referral);
    }

    @Test
    void processReferralWhenOffenderShouldBeRetained() {

        final var referral = mock(OffenderDeletionReferral.class);
        final var retentionCheck = new RetentionCheckManual(RETENTION_REQUIRED);

        referralResolutionService.processReferral(referral,
                List.of(new ActionableRetentionCheck(retentionCheck)));

        verifyPersistence(referral, RETAINED, retentionCheck);
        verify(deletionService, never()).grantDeletion(any());
    }

    @Test
    void processReferralWithPendingRetentionChecks() {

        final var referral = mock(OffenderDeletionReferral.class);
        final var retentionCheck = new RetentionCheckManual(Status.PENDING);
        final var actionableCheck = spy(new ActionableRetentionCheck(retentionCheck));

        referralResolutionService.processReferral(referral, List.of(actionableCheck));

        verifyPersistence(referral, PENDING, retentionCheck);
        verify(actionableCheck).triggerPendingCheck();
        verify(deletionService, never()).grantDeletion(any());
    }

    @Test
    void handlePendingDeletionThrowsWhenNoChecksAreReturned() {

        final var referral = OffenderDeletionReferral.builder().offenderNo("A1234AA").build();

        assertThatThrownBy(() -> referralResolutionService.processReferral(referral, emptyList()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No retention checks have been conducted for offender: 'A1234AA'");

        verify(deletionService, never()).grantDeletion(any());
    }

    private void verifyPersistence(final OffenderDeletionReferral referral,
                                   final ResolutionStatus resolutionStatus,
                                   final RetentionCheck retentionCheck) {

        final var resolution = ArgumentCaptor.forClass(ReferralResolution.class);

        InOrder inOrder = inOrder(referral, referralRepository, deletionService);
        inOrder.verify(referral).setReferralResolution(resolution.capture());
        inOrder.verify(referralRepository).save(referral);

        assertThat(resolution.getValue().getResolutionDateTime()).isEqualTo(NOW);
        assertThat(resolution.getValue().getResolutionStatus()).isEqualTo(resolutionStatus);
        assertThat(resolution.getValue().getRetentionChecks())
                .extracting(RetentionCheck::getCheckType)
                .containsExactly(retentionCheck.getCheckType());
    }
}
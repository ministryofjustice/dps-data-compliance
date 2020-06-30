package uk.gov.justice.hmpps.datacompliance.services.retention;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.config.DataComplianceProperties;
import uk.gov.justice.hmpps.datacompliance.dto.DuplicateResult;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.listeners.dto.DataDuplicateResult;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckIdDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.RetentionCheckRepository;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.referral.ReferralResolutionService;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.ID;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.DISABLED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckAnalyticalPlatformDataDuplicate.DATA_DUPLICATE_AP;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDatabaseDataDuplicate.DATA_DUPLICATE_DB;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckFreeTextSearch.FREE_TEXT_SEARCH;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckIdDataDuplicate.DATA_DUPLICATE_ID;

@ExtendWith(MockitoExtension.class)
class RetentionServiceTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final OffenderNumber DUPLICATE_OFFENDER_NUMBER = new OffenderNumber("B1234BB");
    private static final long DATA_DUPLICATE_CHECK_ID = 1;

    @Mock
    private ManualRetentionService manualRetentionService;

    @Mock
    private PathfinderApiClient pathfinderApiClient;

    @Mock
    private ImageDuplicationDetectionService imageDuplicationDetectionService;

    @Mock
    private DataDuplicationDetectionService dataDuplicationDetectionService;

    @Mock
    private RetentionCheckRepository retentionCheckRepository;

    @Mock
    private ReferralResolutionService referralResolutionService;

    @Mock
    private MoratoriumCheckService moratoriumCheckService;

    private RetentionService service;

    @BeforeEach
    void setUp() {
        service = new RetentionService(
                pathfinderApiClient,
                manualRetentionService,
                imageDuplicationDetectionService,
                dataDuplicationDetectionService,
                retentionCheckRepository,
                referralResolutionService,
                moratoriumCheckService,
                DataComplianceProperties.builder()
                        .idDataDuplicateCheckEnabled(true)
                        .databaseDataDuplicateCheckEnabled(true)
                        .analyticalPlatformDataDuplicateCheckEnabled(true)
                        .build());
    }

    @Test
    void conductRetentionChecksRetentionRequired() {

        final var manualRetention = mock(ManualRetention.class);
        final var imageDuplicate = mock(ImageDuplicate.class);
        final var dataDuplicate = mock(DataDuplicate.class);

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(true);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.of(manualRetention));
        when(imageDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER)).thenReturn(List.of(imageDuplicate));
        when(dataDuplicationDetectionService.searchForAnalyticalPlatformDuplicates(OFFENDER_NUMBER))
                .thenReturn(List.of(dataDuplicate));

        final var retentionChecks = service.conductRetentionChecks(OFFENDER_NUMBER);

        assertThat(retentionChecks).extracting(ActionableRetentionCheck::getRetentionCheck)
                .allMatch(check -> isExpectedStatusWhenChecksEnabled(check, RETENTION_REQUIRED));

        retentionChecks.forEach(ActionableRetentionCheck::triggerPendingCheck);
        verify(dataDuplicationDetectionService).searchForIdDuplicates(eq(OFFENDER_NUMBER), any());
        verify(dataDuplicationDetectionService).searchForDatabaseDuplicates(eq(OFFENDER_NUMBER), any());
        verify(moratoriumCheckService).requestFreeTextSearch(eq(OFFENDER_NUMBER), any());
    }

    @Test
    void conductRetentionChecksRetentionNotRequired() {

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(false);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER)).thenReturn(Optional.empty());
        when(imageDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER)).thenReturn(emptyList());
        when(dataDuplicationDetectionService.searchForAnalyticalPlatformDuplicates(OFFENDER_NUMBER)).thenReturn(emptyList());

        final var retentionChecks = service.conductRetentionChecks(OFFENDER_NUMBER);

        assertThat(retentionChecks).extracting(ActionableRetentionCheck::getRetentionCheck)
                .allMatch(check -> isExpectedStatusWhenChecksEnabled(check, RETENTION_NOT_REQUIRED));

        retentionChecks.forEach(ActionableRetentionCheck::triggerPendingCheck);
        verify(dataDuplicationDetectionService).searchForIdDuplicates(eq(OFFENDER_NUMBER), any());
        verify(dataDuplicationDetectionService).searchForDatabaseDuplicates(eq(OFFENDER_NUMBER), any());
        verify(moratoriumCheckService).requestFreeTextSearch(eq(OFFENDER_NUMBER), any());
    }

    @Test
    void conductRetentionChecksWhenDataDuplicateChecksDisabled() {

        service = new RetentionService(
                pathfinderApiClient,
                manualRetentionService,
                imageDuplicationDetectionService,
                dataDuplicationDetectionService,
                retentionCheckRepository,
                referralResolutionService,
                moratoriumCheckService,
                DataComplianceProperties.builder()
                        .idDataDuplicateCheckEnabled(false)
                        .databaseDataDuplicateCheckEnabled(false)
                        .analyticalPlatformDataDuplicateCheckEnabled(false)
                        .build());

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(false);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER)).thenReturn(Optional.empty());
        when(imageDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER)).thenReturn(emptyList());

        final var retentionChecks = service.conductRetentionChecks(OFFENDER_NUMBER);

        assertThat(retentionChecks).extracting(ActionableRetentionCheck::getRetentionCheck)
                .allMatch(check -> isExpectedStatusWhenChecksDisabled(check, RETENTION_NOT_REQUIRED));

        retentionChecks.forEach(ActionableRetentionCheck::triggerPendingCheck);
        verify(moratoriumCheckService).requestFreeTextSearch(eq(OFFENDER_NUMBER), any());
        verifyNoInteractions(dataDuplicationDetectionService);
    }

    @Test
    void handleDataDuplicateResultWhenDuplicatesAreFound() {

        final var dataDuplicateCheck = persistedDataDuplicateCheck();
        final var dataDuplicates = mockedDataDuplicates();

        service.handleDataDuplicateResult(DataDuplicateResult.builder()
                .offenderIdDisplay(OFFENDER_NUMBER.getOffenderNumber())
                .retentionCheckId(DATA_DUPLICATE_CHECK_ID)
                .duplicateOffenders(List.of(DUPLICATE_OFFENDER_NUMBER.getOffenderNumber()))
                .build(), ID);

        assertThat(dataDuplicateCheck.getDataDuplicates()).containsAll(dataDuplicates);
        assertThat(dataDuplicateCheck.getCheckStatus()).isEqualTo(RETENTION_REQUIRED);

        verify(retentionCheckRepository).save(dataDuplicateCheck);
        verify(referralResolutionService).processUpdatedRetentionCheck(dataDuplicateCheck);
    }

    @Test
    void handleDataDuplicateResultWhenNoDuplicates() {

        final var dataDuplicateCheck = persistedDataDuplicateCheck();

        service.handleDataDuplicateResult(DataDuplicateResult.builder()
                .offenderIdDisplay(OFFENDER_NUMBER.getOffenderNumber())
                .retentionCheckId(DATA_DUPLICATE_CHECK_ID)
                .build(), ID);

        assertThat(dataDuplicateCheck.getDataDuplicates()).isEmpty();
        assertThat(dataDuplicateCheck.getCheckStatus()).isEqualTo(RETENTION_NOT_REQUIRED);

        verify(retentionCheckRepository).save(dataDuplicateCheck);
        verify(referralResolutionService).processUpdatedRetentionCheck(dataDuplicateCheck);
    }

    private RetentionCheckDataDuplicate persistedDataDuplicateCheck() {
        final var dataDuplicateCheck = spy(new RetentionCheckIdDataDuplicate(PENDING));
        dataDuplicateCheck.setRetentionCheckId(DATA_DUPLICATE_CHECK_ID);
        doReturn(OFFENDER_NUMBER).when(dataDuplicateCheck).getOffenderNumber();
        when(retentionCheckRepository.findById(DATA_DUPLICATE_CHECK_ID)).thenReturn(Optional.of(dataDuplicateCheck));
        return dataDuplicateCheck;
    }

    private List<DataDuplicate> mockedDataDuplicates() {
        final var dataDuplicates = List.of(mock(DataDuplicate.class));
        when(dataDuplicationDetectionService.persistDataDuplicates(
                OFFENDER_NUMBER,
                List.of(new DuplicateResult(DUPLICATE_OFFENDER_NUMBER, 100.0)),
                ID))
                .thenReturn(dataDuplicates);
        return dataDuplicates;
    }

    private boolean isExpectedStatusWhenChecksEnabled(final RetentionCheck check,
                                                      final RetentionCheck.Status resolvedStatus) {
        return check.isStatus(isPendingCheck(check) ? PENDING : resolvedStatus);
    }

    private boolean isExpectedStatusWhenChecksDisabled(final RetentionCheck check,
                                                       final RetentionCheck.Status resolvedStatus) {
        return check.isStatus(isDisabledCheck(check) ? DISABLED : isPendingCheck(check) ? PENDING : resolvedStatus);
    }

    private boolean isPendingCheck(final RetentionCheck check) {
        return DATA_DUPLICATE_ID.equals(check.getCheckType())
                || DATA_DUPLICATE_DB.equals(check.getCheckType())
                || FREE_TEXT_SEARCH.equals(check.getCheckType());
    }

    private boolean isDisabledCheck(final RetentionCheck check) {
        return DATA_DUPLICATE_ID.equals(check.getCheckType())
                || DATA_DUPLICATE_DB.equals(check.getCheckType())
                || DATA_DUPLICATE_AP.equals(check.getCheckType());
    }
}

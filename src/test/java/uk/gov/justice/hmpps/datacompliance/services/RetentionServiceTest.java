package uk.gov.justice.hmpps.datacompliance.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckPathfinder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.retention.ActionableRetentionCheck;
import uk.gov.justice.hmpps.datacompliance.services.retention.ManualRetentionService;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_NOT_REQUIRED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

@ExtendWith(MockitoExtension.class)
class RetentionServiceTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");

    @Mock
    private ManualRetentionService manualRetentionService;

    @Mock
    private PathfinderApiClient pathfinderApiClient;

    @Mock
    private ImageDuplicationDetectionService imageDuplicationDetectionService;

    @Mock
    private DataDuplicationDetectionService dataDuplicationDetectionService;

    private RetentionService service;

    @BeforeEach
    void setUp() {
        service = new RetentionService(
                pathfinderApiClient,
                manualRetentionService,
                imageDuplicationDetectionService,
                dataDuplicationDetectionService);
    }

    @Test
    void conductRetentionChecksRetentionRequired() {

        final var manualRetention = mock(ManualRetention.class);
        final var imageDuplicate = mock(ImageDuplicate.class);

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(true);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.of(manualRetention));
        when(imageDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER)).thenReturn(List.of(imageDuplicate));

        final var retentionChecks = service.conductRetentionChecks(OFFENDER_NUMBER);

        assertThat(retentionChecks).extracting(ActionableRetentionCheck::getRetentionCheck)
                .containsExactlyInAnyOrder(
                        new RetentionCheckPathfinder(RETENTION_REQUIRED),
                        new RetentionCheckManual(RETENTION_REQUIRED).setManualRetention(manualRetention),
                        new RetentionCheckImageDuplicate(RETENTION_REQUIRED)
                                .addImageDuplicates(List.of(imageDuplicate)),
                        new RetentionCheckDataDuplicate(PENDING));

        retentionChecks.forEach(ActionableRetentionCheck::triggerPendingCheck);
        verify(dataDuplicationDetectionService).searchForDuplicates(eq(OFFENDER_NUMBER), any());
    }

    @Test
    void conductRetentionChecksRetentionNotRequired() {

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(false);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER)).thenReturn(Optional.empty());
        when(imageDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER)).thenReturn(emptyList());

        final var retentionChecks = service.conductRetentionChecks(OFFENDER_NUMBER);

        assertThat(retentionChecks).extracting(ActionableRetentionCheck::getRetentionCheck)
                .containsExactlyInAnyOrder(
                        new RetentionCheckPathfinder(RETENTION_NOT_REQUIRED),
                        new RetentionCheckManual(RETENTION_NOT_REQUIRED),
                        new RetentionCheckImageDuplicate(RETENTION_NOT_REQUIRED),
                        new RetentionCheckDataDuplicate(PENDING));

        retentionChecks.forEach(ActionableRetentionCheck::triggerPendingCheck);
        verify(dataDuplicationDetectionService).searchForDuplicates(eq(OFFENDER_NUMBER), any());
    }
}

package uk.gov.justice.hmpps.datacompliance.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.ImageDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonPathfinder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.image.ImageDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.services.retention.ManualRetentionService;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        mockEmptyResponses();
        service = new RetentionService(
                pathfinderApiClient,
                manualRetentionService,
                imageDuplicationDetectionService,
                dataDuplicationDetectionService);
    }

    @Test
    void retentionReasonFoundIfOffenderReferredToPathfinder() {

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(true);

        assertThat(service.findRetentionReasons(OFFENDER_NUMBER))
                .containsExactly(new RetentionReasonPathfinder());
    }

    @Test
    void retentionReasonFoundIfManualRetentionExists() {

        final var manualRetention = mock(ManualRetention.class);

        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.of(manualRetention));

        assertThat(service.findRetentionReasons(OFFENDER_NUMBER))
                .containsExactly(new RetentionReasonManual().setManualRetention(manualRetention));
    }

    @Test
    void retentionReasonFoundIfImageDuplicateExists() {

        final var imageDuplicate = mock(ImageDuplicate.class);

        when(imageDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER))
                .thenReturn(List.of(imageDuplicate));

        assertThat(service.findRetentionReasons(OFFENDER_NUMBER))
                .containsExactly(new RetentionReasonDuplicate().addImageDuplicates(List.of(imageDuplicate)));
    }

    @Test
    void retentionReasonFoundIfDataDuplicateExists() {

        final var dataDuplicate = mock(DataDuplicate.class);

        when(dataDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER))
                .thenReturn(List.of(dataDuplicate));

        assertThat(service.findRetentionReasons(OFFENDER_NUMBER))
                .containsExactly(new RetentionReasonDuplicate().addDataDuplicates(List.of(dataDuplicate)));
    }

    @Test
    void retentionReasonRecordsAllReasons() {

        final var manualRetention = mock(ManualRetention.class);
        final var imageDuplicate = mock(ImageDuplicate.class);
        final var dataDuplicate = mock(DataDuplicate.class);

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(true);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.of(manualRetention));
        when(dataDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER)).thenReturn(List.of(dataDuplicate));
        when(imageDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER)).thenReturn(List.of(imageDuplicate));

        assertThat(service.findRetentionReasons(OFFENDER_NUMBER))
                .containsExactlyInAnyOrder(
                        new RetentionReasonPathfinder(),
                        new RetentionReasonManual().setManualRetention(manualRetention),
                        new RetentionReasonDuplicate()
                                .addDataDuplicates(List.of(dataDuplicate))
                                .addImageDuplicates(List.of(imageDuplicate)));
    }

    @Test
    void retentionReasonReturnsEmpty() {
        assertThat(service.findRetentionReasons(OFFENDER_NUMBER)).isEmpty();
    }

    private void mockEmptyResponses() {
        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(false);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER)).thenReturn(Optional.empty());
        when(dataDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER)).thenReturn(emptyList());
        when(imageDuplicationDetectionService.findDuplicatesFor(OFFENDER_NUMBER)).thenReturn(emptyList());
    }
}

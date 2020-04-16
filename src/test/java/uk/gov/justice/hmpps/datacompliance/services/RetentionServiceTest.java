package uk.gov.justice.hmpps.datacompliance.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.pathfinder.PathfinderApiClient;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonManual;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonPathfinder;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.ManualRetention;
import uk.gov.justice.hmpps.datacompliance.services.retention.ManualRetentionService;
import uk.gov.justice.hmpps.datacompliance.services.retention.RetentionService;

import java.util.Optional;

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

    private RetentionService service;

    @BeforeEach
    void setUp() {
        service = new RetentionService(pathfinderApiClient, manualRetentionService);
    }

    @Test
    void retentionReasonFoundIfOffenderReferredToPathfinder() {

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(true);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.empty());

        assertThat(service.findRetentionReasons(OFFENDER_NUMBER))
                .containsExactly(new RetentionReasonPathfinder());
    }

    @Test
    void retentionReasonFoundIfManualRetentionExists() {

        final var manualRetention = mock(ManualRetention.class);

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(false);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.of(manualRetention));

        assertThat(service.findRetentionReasons(OFFENDER_NUMBER))
                .containsExactly(new RetentionReasonManual().setManualRetention(manualRetention));
    }

    @Test
    void retentionReasonRecordsAllReasons() {

        final var manualRetention = mock(ManualRetention.class);

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(true);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.of(manualRetention));

        assertThat(service.findRetentionReasons(OFFENDER_NUMBER))
                .containsExactlyInAnyOrder(
                        new RetentionReasonPathfinder(),
                        new RetentionReasonManual().setManualRetention(manualRetention));
    }

    @Test
    void retentionReasonReturnsEmpty() {

        when(pathfinderApiClient.isReferredToPathfinder(OFFENDER_NUMBER)).thenReturn(false);
        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.empty());

        assertThat(service.findRetentionReasons(OFFENDER_NUMBER)).isEmpty();
    }
}

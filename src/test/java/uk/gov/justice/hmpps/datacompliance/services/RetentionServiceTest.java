package uk.gov.justice.hmpps.datacompliance.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReason;
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

    private RetentionService service;

    @BeforeEach
    void setUp() {
        service = new RetentionService(manualRetentionService);
    }

    @Test
    void isOffenderEligibleForDeletionTrueIfNoManualRetention() {

        final var manualRetention = mock(ManualRetention.class);

        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.of(manualRetention));

        assertThat(service.findRetentionReason(OFFENDER_NUMBER)).contains(RetentionReason.builder()
                .manualRetention(manualRetention)
                .build());
    }

    @Test
    void isOffenderEligibleForDeletionFalseIfManualRetentionExists() {

        when(manualRetentionService.findManualOffenderRetentionWithReasons(OFFENDER_NUMBER))
                .thenReturn(Optional.empty());

        assertThat(service.findRetentionReason(OFFENDER_NUMBER)).isEmpty();
    }
}
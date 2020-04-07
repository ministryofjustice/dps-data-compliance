package uk.gov.justice.hmpps.datacompliance.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;

import static org.assertj.core.api.Assertions.assertThat;
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
        when(manualRetentionService.isManuallyRetained(OFFENDER_NUMBER)).thenReturn(false);

        assertThat(service.isOffenderEligibleForDeletion(OFFENDER_NUMBER)).isTrue();
    }

    @Test
    void isOffenderEligibleForDeletionFalseIfManualRetentionExists() {
        when(manualRetentionService.isManuallyRetained(OFFENDER_NUMBER)).thenReturn(true);

        assertThat(service.isOffenderEligibleForDeletion(OFFENDER_NUMBER)).isFalse();
    }
}
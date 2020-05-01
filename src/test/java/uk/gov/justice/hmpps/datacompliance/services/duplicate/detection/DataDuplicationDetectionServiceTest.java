package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection;

import org.junit.jupiter.api.Test;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;

import static org.assertj.core.api.Assertions.assertThat;

class DataDuplicationDetectionServiceTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");

    private DataDuplicationDetectionService service = new DataDuplicationDetectionService();

    @Test
    void findDuplicatesReturnsEmpty() {
        assertThat(service.findDuplicatesFor(OFFENDER_NUMBER)).isEmpty();
    }
}

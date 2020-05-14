package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DataDuplicationDetectionServiceTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");

    @Mock
    private DataComplianceEventPusher eventPusher;

    private DataDuplicationDetectionService service;

    @BeforeEach
    void setUp() {
        service = new DataDuplicationDetectionService(eventPusher);
    }

    @Test
    void searchForDuplicates() {

        service.searchForDuplicates(OFFENDER_NUMBER, 1L);

        verify(eventPusher).requestDataDuplicateCheck(OFFENDER_NUMBER, 1L);
    }
}

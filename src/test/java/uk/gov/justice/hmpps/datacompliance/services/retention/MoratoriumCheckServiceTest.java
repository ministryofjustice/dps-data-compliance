package uk.gov.justice.hmpps.datacompliance.services.retention;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;

import static org.mockito.Mockito.verify;
import static uk.gov.justice.hmpps.datacompliance.services.retention.MoratoriumCheckService.MORATORIUM_REGEX;

@ExtendWith(MockitoExtension.class)
class MoratoriumCheckServiceTest {

    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final long RETENTION_CHECK_ID = 1;

    @Mock
    private DataComplianceEventPusher eventPusher;

    private MoratoriumCheckService service;

    @BeforeEach
    void setUp() {
        service = new MoratoriumCheckService(eventPusher);
    }

    @Test
    void requestFreeTextSearch() {

        service.requestFreeTextSearch(OFFENDER_NUMBER, RETENTION_CHECK_ID);

        verify(eventPusher).requestFreeTextMoratoriumCheck(OFFENDER_NUMBER, RETENTION_CHECK_ID, MORATORIUM_REGEX);
    }
}

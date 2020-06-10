package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.DataDuplicateRepository;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.ID;

@ExtendWith(MockitoExtension.class)
class DataDuplicationDetectionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final OffenderNumber DUPLICATE_OFFENDER_NUMBER_1 = new OffenderNumber("B1234BB");
    private static final OffenderNumber DUPLICATE_OFFENDER_NUMBER_2 = new OffenderNumber("C1234CC");

    @Mock
    private DataComplianceEventPusher eventPusher;

    @Mock
    private DataDuplicateRepository dataDuplicateRepository;

    private DataDuplicationDetectionService service;

    @BeforeEach
    void setUp() {
        service = new DataDuplicationDetectionService(TimeSource.of(NOW), eventPusher, dataDuplicateRepository);
    }

    @Test
    void searchForIdDuplicates() {

        service.searchForIdDuplicates(OFFENDER_NUMBER, 1L);

        verify(eventPusher).requestIdDataDuplicateCheck(OFFENDER_NUMBER, 1L);
    }

    @Test
    void searchForDbDuplicates() {

        service.searchForDatabaseDuplicates(OFFENDER_NUMBER, 1L);

        verify(eventPusher).requestDatabaseDataDuplicateCheck(OFFENDER_NUMBER, 1L);
    }

    @Test
    void persistDataDuplicates() {

        service.persistDataDuplicates(OFFENDER_NUMBER, List.of(DUPLICATE_OFFENDER_NUMBER_1, DUPLICATE_OFFENDER_NUMBER_2), ID);

        final var dataDuplicateCaptor = ArgumentCaptor.forClass(DataDuplicate.class);
        verify(dataDuplicateRepository, times(2)).save(dataDuplicateCaptor.capture());

        final var dataDuplicates = dataDuplicateCaptor.getAllValues();
        assertThat(dataDuplicates).extracting(DataDuplicate::getDetectionDateTime).allMatch(NOW::equals);
        assertThat(dataDuplicates).extracting(DataDuplicate::getReferenceOffenderNo).allMatch(OFFENDER_NUMBER.getOffenderNumber()::equals);
        assertThat(dataDuplicates).extracting(DataDuplicate::getMethod).allMatch(ID::equals);
        assertThat(dataDuplicates)
                .extracting(DataDuplicate::getDuplicateOffenderNo)
                .containsExactlyInAnyOrder(
                        DUPLICATE_OFFENDER_NUMBER_1.getOffenderNumber(),
                        DUPLICATE_OFFENDER_NUMBER_2.getOffenderNumber());
    }
}

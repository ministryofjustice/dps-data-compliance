package uk.gov.justice.hmpps.datacompliance.services.duplicate.detection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.hmpps.datacompliance.client.duplicate.detection.DuplicateDetectionClient;
import uk.gov.justice.hmpps.datacompliance.dto.DuplicateResult;
import uk.gov.justice.hmpps.datacompliance.dto.OffenderNumber;
import uk.gov.justice.hmpps.datacompliance.events.publishers.sqs.DataComplianceEventPusher;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.DataDuplicateRepository;
import uk.gov.justice.hmpps.datacompliance.services.duplicate.detection.data.DataDuplicationDetectionService;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.ANALYTICAL_PLATFORM;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.ID;

@ExtendWith(MockitoExtension.class)
class DataDuplicationDetectionServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);
    private static final OffenderNumber OFFENDER_NUMBER = new OffenderNumber("A1234AA");
    private static final OffenderNumber DUPLICATE_OFFENDER_NUMBER_1 = new OffenderNumber("B1234BB");
    private static final OffenderNumber DUPLICATE_OFFENDER_NUMBER_2 = new OffenderNumber("C1234CC");
    private static final Double CONFIDENCE = 12.34;

    @Mock
    private DataComplianceEventPusher eventPusher;

    @Mock
    private DataDuplicateRepository dataDuplicateRepository;

    @Mock
    private DuplicateDetectionClient duplicateDetectionClient;

    private DataDuplicationDetectionService service;

    @BeforeEach
    void setUp() {
        service = new DataDuplicationDetectionService(
                TimeSource.of(NOW),
                eventPusher,
                dataDuplicateRepository,
                duplicateDetectionClient);
    }

    @Test
    void searchForIdDuplicates() {

        service.searchForIdDuplicates(OFFENDER_NUMBER, 1L);

        verify(eventPusher).requestIdDataDuplicateCheck(OFFENDER_NUMBER, 1L);
    }

    @Test
    void searchForDatabaseDuplicates() {

        service.searchForDatabaseDuplicates(OFFENDER_NUMBER, 1L);

        verify(eventPusher).requestDatabaseDataDuplicateCheck(OFFENDER_NUMBER, 1L);
    }

    @Test
    void searchForAnalyticalPlatformDuplicates() {

        final var dataDuplicate = mock(DataDuplicate.class);

        when(duplicateDetectionClient.findDuplicatesFor(OFFENDER_NUMBER))
                .thenReturn(Set.of(new DuplicateResult(DUPLICATE_OFFENDER_NUMBER_1, CONFIDENCE)));

        final var dataDuplicateCaptor = ArgumentCaptor.forClass(DataDuplicate.class);
        when(dataDuplicateRepository.save(dataDuplicateCaptor.capture())).thenReturn(dataDuplicate);

        assertThat(service.searchForAnalyticalPlatformDuplicates(OFFENDER_NUMBER)).contains(dataDuplicate);

        assertThat(dataDuplicateCaptor.getValue().getReferenceOffenderNo()).isEqualTo(OFFENDER_NUMBER.getOffenderNumber());
        assertThat(dataDuplicateCaptor.getValue().getDuplicateOffenderNo()).isEqualTo(DUPLICATE_OFFENDER_NUMBER_1.getOffenderNumber());
        assertThat(dataDuplicateCaptor.getValue().getConfidence()).isEqualTo(CONFIDENCE);
        assertThat(dataDuplicateCaptor.getValue().getMethod()).isEqualTo(ANALYTICAL_PLATFORM);
        assertThat(dataDuplicateCaptor.getValue().getDetectionDateTime()).isEqualTo(NOW);
    }

    @Test
    void persistDataDuplicates() {

        final var duplicates = List.of(
                new DuplicateResult(DUPLICATE_OFFENDER_NUMBER_1, CONFIDENCE),
                new DuplicateResult(DUPLICATE_OFFENDER_NUMBER_2, CONFIDENCE));

        service.persistDataDuplicates(OFFENDER_NUMBER, duplicates, ID);

        final var dataDuplicateCaptor = ArgumentCaptor.forClass(DataDuplicate.class);
        verify(dataDuplicateRepository, times(2)).save(dataDuplicateCaptor.capture());

        final var dataDuplicates = dataDuplicateCaptor.getAllValues();
        assertThat(dataDuplicates).extracting(DataDuplicate::getDetectionDateTime).allMatch(NOW::equals);
        assertThat(dataDuplicates).extracting(DataDuplicate::getReferenceOffenderNo).allMatch(OFFENDER_NUMBER.getOffenderNumber()::equals);
        assertThat(dataDuplicates).extracting(DataDuplicate::getMethod).allMatch(ID::equals);
        assertThat(dataDuplicates).extracting(DataDuplicate::getConfidence).allMatch(CONFIDENCE::equals);
        assertThat(dataDuplicates)
                .extracting(DataDuplicate::getDuplicateOffenderNo)
                .containsExactlyInAnyOrder(
                        DUPLICATE_OFFENDER_NUMBER_1.getOffenderNumber(),
                        DUPLICATE_OFFENDER_NUMBER_2.getOffenderNumber());
    }
}

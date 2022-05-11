package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate.Method.ANALYTICAL_PLATFORM;


@Transactional
class DataDuplicateRepositoryTest extends IntegrationTest {

    private static final LocalDateTime DATE_TIME = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private DataDuplicateRepository repository;

    @Test
    void persistDataDuplicateAndRetrieveById() {

        final var dataDuplicate = DataDuplicate.builder()
            .referenceOffenderNo("A1234AA")
            .duplicateOffenderNo("B1234BB")
            .detectionDateTime(DATE_TIME)
            .method(ANALYTICAL_PLATFORM)
            .confidence(98.76)
            .build();

        repository.save(dataDuplicate);
        assertThat(dataDuplicate.getDataDuplicateId()).isNotNull();

        final var retrievedEntity = repository.findById(dataDuplicate.getDataDuplicateId()).orElseThrow();
        assertThat(retrievedEntity.getReferenceOffenderNo()).isEqualTo("A1234AA");
        assertThat(retrievedEntity.getDuplicateOffenderNo()).isEqualTo("B1234BB");
        assertThat(retrievedEntity.getDetectionDateTime()).isEqualTo(DATE_TIME);
        assertThat(retrievedEntity.getMethod()).isEqualTo(ANALYTICAL_PLATFORM);
        assertThat(retrievedEntity.getConfidence()).isEqualTo(98.76);
    }

    @Test
    void persistDataDuplicateWith100PercentConfidence() {

        final var dataDuplicate = DataDuplicate.builder()
            .referenceOffenderNo("A1234AA")
            .duplicateOffenderNo("B1234BB")
            .detectionDateTime(DATE_TIME)
            .method(ANALYTICAL_PLATFORM)
            .confidence(100.00)
            .build();

        repository.save(dataDuplicate);
        assertThat(dataDuplicate.getDataDuplicateId()).isNotNull();

        final var retrievedEntity = repository.findById(dataDuplicate.getDataDuplicateId()).orElseThrow();
        assertThat(retrievedEntity.getConfidence()).isEqualTo(100.00);
    }
}

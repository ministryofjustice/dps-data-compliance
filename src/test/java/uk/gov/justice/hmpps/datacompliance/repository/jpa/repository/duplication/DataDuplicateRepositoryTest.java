package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.duplication.DataDuplicate;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class DataDuplicateRepositoryTest {

    private static final LocalDateTime DATE_TIME = LocalDateTime.now();

    @Autowired
    private DataDuplicateRepository repository;

    @Test
    void persistDataDuplicateAndRetrieveById() {

        final var dataDuplicate = DataDuplicate.builder()
                .referenceOffenderNo("A1234AA")
                .duplicateOffenderNo("B1234BB")
                .detectionDateTime(DATE_TIME)
                .build();

        repository.save(dataDuplicate);
        assertThat(dataDuplicate.getDataDuplicateId()).isNotNull();

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity = repository.findById(dataDuplicate.getDataDuplicateId()).orElseThrow();
        assertThat(retrievedEntity.getReferenceOffenderNo()).isEqualTo("A1234AA");
        assertThat(retrievedEntity.getDuplicateOffenderNo()).isEqualTo("B1234BB");
        assertThat(retrievedEntity.getDetectionDateTime()).isEqualTo(DATE_TIME);
    }
}

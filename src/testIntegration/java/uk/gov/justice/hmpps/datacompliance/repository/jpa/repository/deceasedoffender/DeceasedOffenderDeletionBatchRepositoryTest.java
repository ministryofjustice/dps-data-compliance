package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.deceasedoffender;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.deceasedoffender.DeceasedOffenderDeletionBatch.BatchType;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.JpaRepositoryTest;

import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;

class DeceasedOffenderDeletionBatchRepositoryTest extends JpaRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private DeceasedOffenderDeletionBatchRepository repository;

    @Test
    void persistBatchAndRetrieveById() {
        final var batch = repository.save(DeceasedOffenderDeletionBatch.builder()
            .requestDateTime(NOW)
            .batchType(BatchType.SCHEDULED)
            .referralCompletionDateTime(NOW.plusSeconds(1))
            .build());

        final var retrievedEntity = repository.findById(batch.getBatchId()).orElseThrow();

        assertThat(retrievedEntity.getRequestDateTime()).isEqualTo(NOW);
        assertThat(retrievedEntity.getReferralCompletionDateTime()).isEqualTo(NOW.plusSeconds(1));
        assertThat(retrievedEntity.getBatchType()).isEqualTo(BatchType.SCHEDULED);
    }

    @Test
    @Sql("classpath:seed.data/deceased_offender_deletion_batch.sql")
    void findLatestScheduledBatch() {

        final var latestBatch = repository.findFirstByBatchTypeOrderByRequestDateTimeDesc(BatchType.SCHEDULED).orElseThrow();

        assertThat(latestBatch.getBatchId()).isEqualTo(2);
    }

    @Test
    @Sql("classpath:seed.data/deceased_offender_deletion_batch.sql")
    void findBatchesWithNoReferralCompletionDate() {
        assertThat(repository.findByReferralCompletionDateTimeIsNull())
            .extracting(DeceasedOffenderDeletionBatch::getBatchId)
            .containsExactlyInAnyOrder(2L, 3L);
    }
}

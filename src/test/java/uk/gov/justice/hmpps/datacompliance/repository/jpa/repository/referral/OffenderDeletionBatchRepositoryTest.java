package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TestTransaction;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch.BatchType.SCHEDULED;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class OffenderDeletionBatchRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Autowired
    private OffenderDeletionBatchRepository repository;

    @Test
    void persistBatchAndRetrieveById() {
        final var batch = repository.save(OffenderDeletionBatch.builder()
                .requestDateTime(NOW)
                .referralCompletionDateTime(NOW.plusSeconds(1))
                .windowStartDateTime(NOW.plusSeconds(2))
                .windowEndDateTime(NOW.plusSeconds(3))
                .batchType(SCHEDULED)
                .build());

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var retrievedEntity = repository.findById(batch.getBatchId()).orElseThrow();

        assertThat(retrievedEntity.getRequestDateTime()).isEqualTo(NOW);
        assertThat(retrievedEntity.getReferralCompletionDateTime()).isEqualTo(NOW.plusSeconds(1));
        assertThat(retrievedEntity.getWindowStartDateTime()).isEqualTo(NOW.plusSeconds(2));
        assertThat(retrievedEntity.getWindowEndDateTime()).isEqualTo(NOW.plusSeconds(3));
        assertThat(retrievedEntity.getBatchType()).isEqualTo(SCHEDULED);
    }

    @Test
    @Sql("offender_deletion_batch.sql")
    void findLatestScheduledBatch() {

        final var latestBatch = repository.findFirstByBatchTypeOrderByRequestDateTimeDesc(SCHEDULED).orElseThrow();

        assertThat(latestBatch.getBatchId()).isEqualTo(2);
    }

    @Test
    @Sql("offender_deletion_batch.sql")
    void findBatchesWithNoReferralCompletionDate() {
        assertThat(repository.findByReferralCompletionDateTimeIsNull())
                .extracting(OffenderDeletionBatch::getBatchId)
                .containsExactlyInAnyOrder(2L, 3L);
    }
}

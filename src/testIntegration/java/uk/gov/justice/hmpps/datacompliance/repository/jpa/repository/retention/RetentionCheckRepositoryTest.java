package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckIdDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.JpaRepositoryTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.DataDuplicateRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

class RetentionCheckRepositoryTest extends JpaRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private DataDuplicateRepository dataDuplicateRepository;

    @Autowired
    private RetentionCheckRepository retentionCheckRepository;

    @Test
    @Sql("classpath:seed.data/data_duplicate.sql")
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/retention_check_2.sql")
    void retrieveByIdAndUpdate() {

        final var retentionCheck = getRetentionCheck();

        assertThat(retentionCheck.getCheckStatus()).isEqualTo(PENDING);
        assertThat(retentionCheck.getDataDuplicates()).isEmpty();

        retentionCheck.setCheckStatus(RETENTION_REQUIRED);
        retentionCheck.addDataDuplicates(List.of(dataDuplicateRepository.findById(1L).orElseThrow()));
        retentionCheckRepository.save(retentionCheck);

        final var persistedCheck = getRetentionCheck();
        assertThat(persistedCheck.getCheckStatus()).isEqualTo(RETENTION_REQUIRED);
        assertThat(persistedCheck.getDataDuplicates()).hasSize(1);
    }

    private RetentionCheckDataDuplicate getRetentionCheck() {
        return retentionCheckRepository.findById(1L)
            .map(RetentionCheckIdDataDuplicate.class::cast)
            .orElseThrow();
    }
}

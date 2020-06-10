package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention;

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
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheckIdDataDuplicate;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.duplication.DataDuplicateRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class RetentionCheckRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.now();

    @Autowired
    private DataDuplicateRepository dataDuplicateRepository;

    @Autowired
    private RetentionCheckRepository retentionCheckRepository;

    @Test
    @Sql("data_duplicate.sql")
    @Sql("offender_deletion_batch.sql")
    @Sql("offender_deletion_referral.sql")
    @Sql("referral_resolution.sql")
    @Sql("retention_check.sql")
    void retrieveByIdAndUpdate() {

        final var retentionCheck = getRetentionCheck();

        assertThat(retentionCheck.getCheckStatus()).isEqualTo(PENDING);
        assertThat(retentionCheck.getDataDuplicates()).isEmpty();

        retentionCheck.setCheckStatus(RETENTION_REQUIRED);
        retentionCheck.addDataDuplicates(List.of(dataDuplicateRepository.findById(1L).orElseThrow()));
        retentionCheckRepository.save(retentionCheck);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

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

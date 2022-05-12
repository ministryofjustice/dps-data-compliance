package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.JpaRepositoryTest;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.manual.RetentionReasonCode.Code.HIGH_PROFILE;

class RetentionReasonCodeRepositoryTest extends JpaRepositoryTest {

    @Autowired
    private RetentionReasonCodeRepository repository;

    @Test
    @Sql("classpath:seed.data/retention_reason_code.sql")
    void retrieveRetentionReasonCodeById() {

        final var retrievedEntity = repository.findById(HIGH_PROFILE).orElseThrow();

        assertThat(retrievedEntity.getDisplayName()).isEqualTo("High Profile Offenders");
        assertThat(retrievedEntity.getAllowReasonDetails()).isFalse();
        assertThat(retrievedEntity.getDisplayOrder()).isEqualTo(1);
    }
}

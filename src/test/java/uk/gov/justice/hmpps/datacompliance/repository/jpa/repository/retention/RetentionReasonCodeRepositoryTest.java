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

import javax.transaction.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionReasonCode.Code.HIGH_PROFILE;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
class RetentionReasonCodeRepositoryTest {

    @Autowired
    private RetentionReasonCodeRepository repository;

    @Test
    @Sql("retention_reason_code.sql")
    void retrieveRetentionReasonCodeById() {

        final var retrievedEntity = repository.findById(HIGH_PROFILE).orElseThrow();

        assertThat(retrievedEntity.getDisplayName()).isEqualTo("High Profile Offenders");
        assertThat(retrievedEntity.getAllowReasonDetails()).isFalse();
        assertThat(retrievedEntity.getDisplayOrder()).isEqualTo(1);
    }
}

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

import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.RETAINED;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest
@Transactional
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
class ReferralResolutionRepositoryTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private ReferralResolutionRepository repository;

    @Test
    @Sql("offender_deletion_batch.sql")
    @Sql("offender_deletion_referral.sql")
    @Sql("referral_resolution.sql")
    void retrieveByIdAndUpdate() {

        final var resolution = repository.findById(1L).orElseThrow();
        resolution.setResolutionDateTime(NOW);
        resolution.setResolutionStatus(RETAINED);
        repository.save(resolution);

        TestTransaction.flagForCommit();
        TestTransaction.end();
        TestTransaction.start();

        final var persistedResolution = repository.findById(1L).orElseThrow();
        assertThat(persistedResolution.getResolutionDateTime()).isEqualTo(NOW);
        assertThat(persistedResolution.getResolutionStatus()).isEqualTo(RETAINED);
    }
}

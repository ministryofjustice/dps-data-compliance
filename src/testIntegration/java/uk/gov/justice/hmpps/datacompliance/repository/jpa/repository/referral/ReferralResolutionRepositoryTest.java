package uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;

import javax.transaction.Transactional;
import java.time.LocalDateTime;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.RETAINED;

@Transactional
class ReferralResolutionRepositoryTest extends IntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.now().truncatedTo(MILLIS);

    @Autowired
    private ReferralResolutionRepository repository;

    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    void retrieveByIdAndUpdate() {

        final var resolution = repository.findById(1L).orElseThrow();
        resolution.setResolutionDateTime(NOW);
        resolution.setResolutionStatus(RETAINED);
        repository.save(resolution);

        final var persistedResolution = repository.findById(1L).orElseThrow();
        assertThat(persistedResolution.getResolutionDateTime()).isEqualTo(NOW);
        assertThat(persistedResolution.getResolutionStatus()).isEqualTo(RETAINED);
    }
}

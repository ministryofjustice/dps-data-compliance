package uk.gov.justice.hmpps.datacompliance.services.referral;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.ReferralResolutionRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.RetentionCheckRepository;

import javax.transaction.Transactional;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.RETAINED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

class ReferralResolutionServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private ReferralResolutionRepository referralResolutionRepository;

    @Autowired
    private RetentionCheckRepository retentionCheckRepository;

    @Autowired
    private ReferralResolutionService referralResolutionService;

    @Test
    @Sql("offender_deletion_batch.sql")
    @Sql("offender_deletion_referral.sql")
    @Sql("referral_resolution.sql")
    @Sql("retention_check.sql")
    void processUpdatedRetentionCheckResultsInRetention() {

        assertThat(referralResolutionRepository.findById(1L).orElseThrow().getResolutionStatus()).isEqualTo(PENDING);

        CompletableFuture.allOf(
                runAsync(() -> updateAndProcessCheck(1L)),
                runAsync(() -> updateAndProcessCheck(2L)))
                .join();

        assertThat(referralResolutionRepository.findById(1L).orElseThrow().getResolutionStatus()).isEqualTo(RETAINED);
    }

    @Transactional
    private void updateAndProcessCheck(final long retentionCheckId) {

        final var check = retentionCheckRepository.findById(retentionCheckId).orElseThrow();
        check.setCheckStatus(RETENTION_REQUIRED);
        retentionCheckRepository.save(check);

        referralResolutionService.processUpdatedRetentionCheck(check);
    }
}

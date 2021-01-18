package uk.gov.justice.hmpps.datacompliance.services.referral;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.jdbc.Sql;
import uk.gov.justice.hmpps.datacompliance.IntegrationTest;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.ReferralResolutionRepository;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.retention.RetentionCheckRepository;

import javax.transaction.Transactional;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.runAsync;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.PENDING;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.ReferralResolution.ResolutionStatus.RETAINED;
import static uk.gov.justice.hmpps.datacompliance.repository.jpa.model.retention.RetentionCheck.Status.RETENTION_REQUIRED;

class ReferralResolutionServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private ReferralResolutionRepository referralResolutionRepository;

    @Autowired
    private RetentionCheckRepository retentionCheckRepository;

    @SpyBean
    private ReferralResolutionService referralResolutionService;

    @Test
    @Sql("classpath:seed.data/offender_deletion_batch.sql")
    @Sql("classpath:seed.data/offender_deletion_referral.sql")
    @Sql("classpath:seed.data/referral_resolution.sql")
    @Sql("classpath:seed.data/retention_check.sql")
    void processUpdatedRetentionCheckResultsInRetention() {

        assertThat(referralResolutionRepository.findById(1L).orElseThrow().getResolutionStatus()).isEqualTo(PENDING);

        delayFirstProcessFromCompletingTransaction();
        CompletableFuture.allOf(
                runAsync(() -> updateAndProcessCheck(1L)),
                runAsync(() -> updateAndProcessCheck(2L)))
                .join();

        assertThat(referralResolutionRepository.findById(1L).orElseThrow().getResolutionStatus()).isEqualTo(RETAINED);
    }

    @Transactional
    void updateAndProcessCheck(final long retentionCheckId) {

        // Give the first process a head start:
        if (retentionCheckId == 2) {
            delay(100);
        }

        final var check = retentionCheckRepository.findById(retentionCheckId).orElseThrow();
        check.setCheckStatus(RETENTION_REQUIRED);
        retentionCheckRepository.save(check);

        referralResolutionService.processUpdatedRetentionCheck(check);
    }

    // The first process has a head start so will call the
    // findResolution method first.
    private void delayFirstProcessFromCompletingTransaction() {
        doAnswer(invocation -> {
            final var result = invocation.callRealMethod();
            delay(150);
            return result;
        })
        .doAnswer(InvocationOnMock::callRealMethod)
        .when(referralResolutionService).findResolution(any());
    }

    private void delay(final long millis) {
        try {
            Thread.sleep(millis);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

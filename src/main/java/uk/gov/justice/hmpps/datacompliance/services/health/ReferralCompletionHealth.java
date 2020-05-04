package uk.gov.justice.hmpps.datacompliance.services.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.Duration;

import static java.util.stream.Collectors.toSet;
import static org.springframework.boot.actuate.health.Health.down;
import static org.springframework.boot.actuate.health.Health.up;

@Slf4j
@Component
public class ReferralCompletionHealth implements HealthIndicator {

    private final OffenderDeletionBatchRepository repository;
    private final Duration tolerance;
    private final TimeSource timeSource;

    public ReferralCompletionHealth(final OffenderDeletionBatchRepository repository,
                                    @Value("${inbound.referral.completion.tolerance:1h}") final Duration tolerance,
                                    final TimeSource timeSource) {
        this.tolerance = tolerance;
        this.repository = repository;
        this.timeSource = timeSource;
    }

    @Override
    public Health health() {

        try {
            return referralCompletionHealth();
        } catch (Exception e) {
            log.error("Unable to query for referral completion due to exception:", e);
            return down().withException(e).build();
        }
    }

    private Health referralCompletionHealth() {

        final var batchesOverdue = repository.findByReferralCompletionDateTimeIsNull().stream()
                .filter(batch -> batch.getRequestDateTime().isBefore(timeSource.nowAsLocalDateTime().minus(tolerance)))
                .map(OffenderDeletionBatch::getBatchId)
                .collect(toSet());

        if (!batchesOverdue.isEmpty()) {
            return down().withDetail("batchesOverdue", batchesOverdue).build();
        }

        return up().build();
    }
}

package uk.gov.justice.hmpps.datacompliance.services.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Status;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.model.referral.OffenderDeletionBatch;
import uk.gov.justice.hmpps.datacompliance.repository.jpa.repository.referral.OffenderDeletionBatchRepository;
import uk.gov.justice.hmpps.datacompliance.utils.TimeSource;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReferralCompletionHealthTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final Duration TOLERANCE = Duration.ofHours(1);

    @Mock
    private OffenderDeletionBatchRepository repository;

    private ReferralCompletionHealth referralCompletionHealth;

    @BeforeEach
    void setUp() {
        referralCompletionHealth = new ReferralCompletionHealth(repository, TOLERANCE, TimeSource.of(NOW));
    }

    @Test
    void reportsHealthyWhenNoBatchesAreMissingCompletionDate() {

        when(repository.findByReferralCompletionDateTimeIsNull()).thenReturn(emptyList());

        final var health = referralCompletionHealth.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsHealthyWhenRequestDateTimeWithinTolerance() {

        when(repository.findByReferralCompletionDateTimeIsNull()).thenReturn(List.of(
                OffenderDeletionBatch.builder()
                        .requestDateTime(NOW.minus(TOLERANCE))
                        .build()));

        final var health = referralCompletionHealth.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void reportsDownWhenRequestDateTimeOutsideTolerance() {

        when(repository.findByReferralCompletionDateTimeIsNull()).thenReturn(List.of(
                OffenderDeletionBatch.builder()
                        .batchId(1L)
                        .requestDateTime(NOW.minus(TOLERANCE).minusSeconds(1))
                        .build()));

        final var health = referralCompletionHealth.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("batchesOverdue")).isEqualTo(Set.of(1L));
    }

    @Test
    void reportsDownWhenExceptionThrown() {

        when(repository.findByReferralCompletionDateTimeIsNull()).thenThrow(new RuntimeException("error!"));

        final var health = referralCompletionHealth.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat((String) health.getDetails().get("error")).contains("error!");
    }
}
